package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TelegramCallbackTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void parsesAButtonPress() throws Exception {
        final var node = JSON.readTree("""
                {"update_id":1,"callback_query":{"id":"abc","data":"cx:1",
                 "message":{"message_id":42,"chat":{"id":777,"type":"private"}}}}""");

        assertThat(TelegramCallback.parse(node)).contains(new TelegramCallback("abc", "777", "private", 42L, "cx:1"));
    }

    @Test
    void ignoresPlainMessagesAndIncompleteCallbacks() throws Exception {
        assertThat(TelegramCallback.parse(JSON.readTree("""
                {"update_id":1,"message":{"text":"hi","chat":{"id":1}}}"""))).isEmpty();
        assertThat(TelegramCallback.parse(JSON.readTree("""
                {"update_id":1,"callback_query":{"id":"abc","data":"x"}}"""))).isEmpty();
        assertThat(TelegramCallback.parse(null)).isEmpty();
    }
}
