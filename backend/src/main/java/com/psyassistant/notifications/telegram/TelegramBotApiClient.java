package com.psyassistant.notifications.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link TelegramClient} backed by Spring's {@link RestClient}.
 *
 * <p>The bot token is part of the request path; it is never logged and is stripped from errors.
 */
@Component
public class TelegramBotApiClient implements TelegramClient {

    private static final String API = "https://api.telegram.org/bot";

    private final TelegramProperties props;
    private final RestClient client;

    /**
     * Creates the client.
     *
     * @param props Telegram configuration
     */
    public TelegramBotApiClient(final TelegramProperties props) {
        this.props = props;
        final Duration read = Duration.ofSeconds(props.getPollTimeoutSeconds() + 10L);
        this.client = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofSeconds(5)).withReadTimeout(read)))
                .build();
    }

    @Override
    public void sendMessage(final String chatId, final String text) {
        call("sendMessage", Map.of("chat_id", chatId, "text", text, "disable_web_page_preview", true));
    }

    @Override
    public void sendMessage(final String chatId, final String text, final List<Button> buttons) {
        call("sendMessage", Map.of("chat_id", chatId, "text", text, "disable_web_page_preview", true,
                "reply_markup", keyboard(buttons)));
    }

    @Override
    public void editMessage(final String chatId, final long messageId, final String text,
                            final List<Button> buttons) {
        call("editMessageText", Map.of("chat_id", chatId, "message_id", messageId, "text", text,
                "disable_web_page_preview", true, "reply_markup", keyboard(buttons)));
    }

    @Override
    public void answerCallbackQuery(final String callbackQueryId) {
        call("answerCallbackQuery", Map.of("callback_query_id", callbackQueryId));
    }

    private static Map<String, Object> keyboard(final List<Button> buttons) {
        final List<Map<String, String>> row = buttons.stream()
                .map(b -> Map.of("text", b.text(), "callback_data", b.callbackData()))
                .toList();
        return Map.of("inline_keyboard", row.isEmpty() ? List.of() : List.of(row));
    }

    @Override
    public List<JsonNode> getUpdates(final long offset, final int timeoutSeconds) {
        final JsonNode result = call("getUpdates", Map.of(
                "offset", offset, "timeout", timeoutSeconds, "allowed_updates", List.of("message", "callback_query")));
        final List<JsonNode> updates = new ArrayList<>();
        if (result != null && result.path("result").isArray()) {
            result.path("result").forEach(updates::add);
        }
        return updates;
    }

    @Override
    public void setWebhook(final String url, final String secret) {
        call("setWebhook", Map.of("url", url, "secret_token", secret,
                "allowed_updates", List.of("message", "callback_query")));
    }

    @Override
    public void deleteWebhook() {
        call("deleteWebhook", Map.of("drop_pending_updates", false));
    }

    private JsonNode call(final String method, final Map<String, ?> body) {
        try {
            return client.post()
                    .uri(API + props.getBotToken() + "/" + method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            final JsonNode err = parseError(e);
            throw new TelegramApiException(
                    "Telegram " + method + " failed: " + e.getStatusCode().value() + " "
                            + err.path("description").asText(""),
                    e.getStatusCode().value(),
                    err.path("parameters").path("retry_after").asInt(0));
        } catch (RuntimeException e) {
            throw new TelegramApiException("Telegram " + method + " transport error: "
                    + e.getClass().getSimpleName(), 0, 0);
        }
    }

    private JsonNode parseError(final RestClientResponseException e) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
        } catch (Exception ignored) {
            return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        }
    }
}
