package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ClientBotWebhookControllerTest {

    private static final String UPDATE = """
            {"update_id": 7, "message": {"text": "/start x", "chat": {"id": 42, "type": "private"}}}""";

    private final ObjectMapper mapper = new ObjectMapper();
    private TelegramProperties props;
    private ClientBotUpdateHandler handler;
    private ClientBotWebhookController controller;

    @BeforeEach
    void setUp() {
        props = new TelegramProperties();
        props.setWebhookSecret("therapist-secret");
        props.getClientBot().setWebhookSecret("client-secret");
        handler = mock(ClientBotUpdateHandler.class);
        controller = new ClientBotWebhookController(props, handler);
    }

    @Test
    void theTherapistBotsSecretDoesNotOpenTheClientEndpoint() throws Exception {
        assertThat(controller.receive("therapist-secret", mapper.readTree(UPDATE)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.receive(null, mapper.readTree(UPDATE)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(handler, never()).handle(any());
    }

    @Test
    void blankClientSecretRejectsEverything() throws Exception {
        props.getClientBot().setWebhookSecret("");

        assertThat(controller.receive("", mapper.readTree(UPDATE)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void validSecretDispatchesTheUpdate() throws Exception {
        assertThat(controller.receive("client-secret", mapper.readTree(UPDATE)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        verify(handler).handle(new TelegramUpdate(7, "42", "private", "/start x"));
    }
}
