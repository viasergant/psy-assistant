package com.psyassistant.crm.clients;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.common.audit.AuditLogService;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import com.psyassistant.crm.clients.dto.ClientDetailDto;
import com.psyassistant.crm.clients.dto.UpdateClientProfileRequest;
import com.psyassistant.crm.clients.dto.UpdateClientTagsRequest;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link ClientController}.
 */
@WebMvcTest(controllers = ClientController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class ClientControllerTest {

    private static final String BASE = "/api/v1/clients";
    private static final UUID CLIENT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientProfileService clientProfileService;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    void getClientReturns200ForManageClients() throws Exception {
        when(clientProfileService.getClientProfile(CLIENT_ID)).thenReturn(sampleDto());

        mockMvc.perform(get(BASE + "/" + CLIENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_CLIENTS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CLIENT_ID.toString()));
    }

    @Test
    void getClientReturns403ForMissingAuthorities() throws Exception {
        mockMvc.perform(get(BASE + "/" + CLIENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_LEADS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void putClientReturns200ForManageClients() throws Exception {
        when(clientProfileService.updateClientProfile(eq(CLIENT_ID), any(), any(), any()))
                .thenReturn(sampleDto());

        mockMvc.perform(put(BASE + "/" + CLIENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_CLIENTS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Anna Kovalenko"));
    }

    @Test
    void putClientReturns403ForReadOnlyAuthority() throws Exception {
        mockMvc.perform(put(BASE + "/" + CLIENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_CLIENTS_ALL"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void putClientReturns409OnOptimisticLockFailure() throws Exception {
        when(clientProfileService.updateClientProfile(eq(CLIENT_ID), any(), any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Client.class, CLIENT_ID));

        mockMvc.perform(put(BASE + "/" + CLIENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_CLIENTS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_UPDATE"));
    }

    @Test
    void patchTagsReturns200ForManageClients() throws Exception {
        when(clientProfileService.updateClientTags(eq(CLIENT_ID), any(), any(), any()))
                .thenReturn(sampleDto());

        mockMvc.perform(patch(BASE + "/" + CLIENT_ID + "/tags")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_CLIENTS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateClientTagsRequest(
                                2L,
                                List.of("adult", "priority")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CLIENT_ID.toString()));
    }

    @Test
    void postPhotoReturns200ForManageClients() throws Exception {
        when(clientProfileService.uploadClientPhoto(eq(CLIENT_ID), eq(2L), any(), any(), any()))
                .thenReturn(sampleDto());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "client.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        mockMvc.perform(multipart(BASE + "/" + CLIENT_ID + "/photo")
                        .file(file)
                        .param("version", "2")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_CLIENTS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CLIENT_ID.toString()));
    }

    @Test
    void getPhotoReturnsImagePayload() throws Exception {
        when(clientProfileService.getClientPhoto(CLIENT_ID))
                .thenReturn(new ClientPhotoData("image-bits".getBytes(), MediaType.IMAGE_PNG_VALUE));

        mockMvc.perform(get(BASE + "/" + CLIENT_ID + "/photo")
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_CLIENTS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void getClientReturns404WhenMissing() throws Exception {
        when(clientProfileService.getClientProfile(CLIENT_ID))
                .thenThrow(new EntityNotFoundException("Client not found"));

        mockMvc.perform(get(BASE + "/" + CLIENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("MANAGE_CLIENTS"))
                                .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound());
    }

    private ClientDetailDto sampleDto() {
                Client client = new Client("Anna Kovalenko");
                client.setClientCode("CL-1234ABCD");
                ReflectionTestUtils.setField(client, "id", CLIENT_ID);
                ReflectionTestUtils.setField(client, "version", 2L);
                ReflectionTestUtils.setField(client, "createdAt", Instant.now());
                ReflectionTestUtils.setField(client, "updatedAt", Instant.now());
                return ClientDetailDto.from(client, List.of(), null, true, true, true);
    }

    private UpdateClientProfileRequest updateRequest() {
        return new UpdateClientProfileRequest(
                2L,
                "Anna Kovalenko",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
