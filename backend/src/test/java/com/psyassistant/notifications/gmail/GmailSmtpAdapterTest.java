package com.psyassistant.notifications.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import com.psyassistant.notifications.EmailMessage;
import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.notifications.log.EmailDeliveryLog;
import com.psyassistant.notifications.log.EmailDeliveryLogRepository;
import com.psyassistant.notifications.log.EmailDeliveryStatus;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link GmailSmtpAdapter}.
 */
@ExtendWith(MockitoExtension.class)
class GmailSmtpAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailDeliveryLogRepository repository;

    @Mock
    private MessageSource messageSource;

    private GmailEmailProperties properties;
    private GmailSmtpAdapter adapter;

    private static final String TEST_RECIPIENT = "client@example.com";
    private static final String TEST_SUBJECT_KEY = "appointment.reminder";

    @BeforeEach
    void setUp() {
        properties = new GmailEmailProperties();
        properties.setSenderAddress("noreply@psyassistant.com");
        properties.setSenderName("PSY Assistant");
        adapter = new GmailSmtpAdapter(mailSender, properties, repository, messageSource);
        ReflectionTestUtils.setField(adapter, "smtpUsername", "smtp@gmail.com");
        ReflectionTestUtils.setField(adapter, "smtpPassword", "secret");
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageSource.getMessage(any(String.class), isNull(), any(String.class), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(2));
    }

    @Nested
    @DisplayName("queue()")
    class QueueTests {

        @Test
        @DisplayName("persists PENDING row when credentials are configured")
        void persistsPendingRow() {
            final EmailMessage message = new EmailMessage(
                    TEST_RECIPIENT, NotificationEventType.WELCOME,
                    TEST_SUBJECT_KEY, "");

            adapter.queue(message);

            final ArgumentCaptor<EmailDeliveryLog> captor =
                    ArgumentCaptor.forClass(EmailDeliveryLog.class);
            verify(repository).save(captor.capture());
            final EmailDeliveryLog saved = captor.getValue();

            assertThat(saved.getStatus()).isEqualTo(EmailDeliveryStatus.PENDING);
            assertThat(saved.getEventType()).isEqualTo(NotificationEventType.WELCOME);
            assertThat(saved.getSubjectTemplateKey()).isEqualTo(TEST_SUBJECT_KEY);
            assertThat(saved.getRecipientAddressHash()).hasSize(64);
        }

        @Test
        @DisplayName("hashes recipient address — hash is 64 hex chars (SHA-256)")
        void hashesRecipientAddress() {
            adapter.queue(new EmailMessage(
                    TEST_RECIPIENT, NotificationEventType.WELCOME, TEST_SUBJECT_KEY, ""));

            final ArgumentCaptor<EmailDeliveryLog> captor =
                    ArgumentCaptor.forClass(EmailDeliveryLog.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getRecipientAddressHash()).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("persists CONFIG_ERROR and throws EmailConfigException when sender address blank")
        void configErrorWhenSenderBlank() {
            properties.setSenderAddress("");

            assertThatThrownBy(() -> adapter.queue(
                    new EmailMessage(TEST_RECIPIENT, NotificationEventType.WELCOME,
                            TEST_SUBJECT_KEY, "")))
                    .isInstanceOf(EmailConfigException.class);

            final ArgumentCaptor<EmailDeliveryLog> captor =
                    ArgumentCaptor.forClass(EmailDeliveryLog.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(EmailDeliveryStatus.CONFIG_ERROR);
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("persists CONFIG_ERROR and throws EmailConfigException when SMTP password blank")
        void configErrorWhenPasswordBlank() {
            ReflectionTestUtils.setField(adapter, "smtpPassword", "");

            assertThatThrownBy(() -> adapter.queue(
                    new EmailMessage(TEST_RECIPIENT, NotificationEventType.WELCOME,
                            TEST_SUBJECT_KEY, "")))
                    .isInstanceOf(EmailConfigException.class);

            final ArgumentCaptor<EmailDeliveryLog> captor =
                    ArgumentCaptor.forClass(EmailDeliveryLog.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(EmailDeliveryStatus.CONFIG_ERROR);
        }

        @Test
        @DisplayName("persists CONFIG_ERROR and throws EmailConfigException when SMTP username blank")
        void configErrorWhenUsernameBlank() {
            ReflectionTestUtils.setField(adapter, "smtpUsername", "");

            assertThatThrownBy(() -> adapter.queue(
                    new EmailMessage(TEST_RECIPIENT, NotificationEventType.WELCOME,
                            TEST_SUBJECT_KEY, "")))
                    .isInstanceOf(EmailConfigException.class);

            final ArgumentCaptor<EmailDeliveryLog> captor =
                    ArgumentCaptor.forClass(EmailDeliveryLog.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(EmailDeliveryStatus.CONFIG_ERROR);
        }
    }

    @Nested
    @DisplayName("send()")
    class SendTests {

        private EmailDeliveryLog buildEntry() {
            return new EmailDeliveryLog(
                    NotificationEventType.APPOINTMENT_REMINDER,
                    TEST_RECIPIENT,
                    "abc123",
                    TEST_SUBJECT_KEY,
                    "Click here to reset your password: https://example.com/reset?token=abc");
        }

        @Test
        @DisplayName("sends mail and transitions to SENT with sentAt populated")
        void sendsMailAndMarksSent() {
            final EmailDeliveryLog entry = buildEntry();

            adapter.send(entry);

            verify(mailSender).send(any(SimpleMailMessage.class));
            assertThat(entry.getStatus()).isEqualTo(EmailDeliveryStatus.SENT);
            assertThat(entry.getSentAt()).isNotNull();
            assertThat(entry.getProviderMessageId()).startsWith("smtp-");
        }

        @Test
        @DisplayName("sets CONFIG_ERROR status when credentials absent at send time")
        void configErrorAtSendTime() {
            ReflectionTestUtils.setField(adapter, "smtpPassword", "");
            final EmailDeliveryLog entry = buildEntry();

            adapter.send(entry);

            verify(mailSender, never()).send(any(SimpleMailMessage.class));
            assertThat(entry.getStatus()).isEqualTo(EmailDeliveryStatus.CONFIG_ERROR);
        }
    }

    @Nested
    @DisplayName("isConfigured()")
    class IsConfiguredTests {

        @Test
        @DisplayName("returns true when all credentials are present")
        void returnsTrueWhenConfigured() {
            assertThat(adapter.isConfigured()).isTrue();
        }

        @Test
        @DisplayName("returns false when sender address is blank")
        void returnsFalseWhenSenderBlank() {
            properties.setSenderAddress("");
            assertThat(adapter.isConfigured()).isFalse();
        }

        @Test
        @DisplayName("returns false when SMTP password is null")
        void returnsFalseWhenPasswordNull() {
            ReflectionTestUtils.setField(adapter, "smtpPassword", null);
            assertThat(adapter.isConfigured()).isFalse();
        }
    }
}
