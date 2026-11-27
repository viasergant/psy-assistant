package com.psyassistant.contacts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer tests for the public and global-admin contacts endpoints. */
@WebMvcTest(controllers = {PublicContactsController.class, GlobalAdminContactsController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles({"solo", "test"})
class ContactsControllerTest {

    private static final String PUBLIC = "/api/v1/contacts";
    private static final String ADMIN = "/api/v1/global-admin/contacts";
    private static final String VALID_BODY =
            "{\"phone\":\"+380441234567\",\"email\":\"help@example.com\",\"telegramChannels\":[\"@psy_help\"]}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlatformContactsService service;

    @MockitoBean
    private AuditLogService auditLogService;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor globalAdmin() {
        return jwt().authorities(new SimpleGrantedAuthority("GLOBAL_ADMIN"))
                .jwt(j -> j.subject(UUID.randomUUID().toString()));
    }

    @Test
    void publicEndpointIsAccessibleWithoutAuthentication() throws Exception {
        when(service.get()).thenReturn(new ContactsResponse("+1 555 0100", "a@b.co", List.of("https://t.me/psy_help")));

        mockMvc.perform(get(PUBLIC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@b.co"))
                .andExpect(jsonPath("$.telegramChannels[0]").value("https://t.me/psy_help"));
    }

    @Test
    void adminGetRequiresAuthentication() throws Exception {
        mockMvc.perform(get(ADMIN)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminUpdateIsForbiddenWithoutGlobalAdminAuthority() throws Exception {
        mockMvc.perform(put(ADMIN).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMINISTRATOR"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void adminUpdateSucceedsForGlobalAdmin() throws Exception {
        when(service.update(any(ContactsRequest.class)))
                .thenReturn(new ContactsResponse("+380441234567", "help@example.com",
                        List.of("https://t.me/psy_help")));

        mockMvc.perform(put(ADMIN).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY).with(globalAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+380441234567"));
    }

    @Test
    void adminUpdateRejectsInvalidEmailAndTelegramEntries() throws Exception {
        String badEmail = "{\"email\":\"not-an-email\"}";
        String badChannel = "{\"telegramChannels\":[\"https://evil.example/x\"]}";

        mockMvc.perform(put(ADMIN).contentType(MediaType.APPLICATION_JSON).content(badEmail).with(globalAdmin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(ADMIN).contentType(MediaType.APPLICATION_JSON).content(badChannel).with(globalAdmin()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void adminUpdateRejectsMoreThanTenChannels() throws Exception {
        String body = "{\"telegramChannels\":[" + "\"@channel_a\",".repeat(10) + "\"@channel_b\"]}";

        mockMvc.perform(put(ADMIN).contentType(MediaType.APPLICATION_JSON).content(body).with(globalAdmin()))
                .andExpect(status().isBadRequest());
    }
}
