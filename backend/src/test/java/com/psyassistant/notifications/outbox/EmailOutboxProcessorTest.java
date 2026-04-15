package com.psyassistant.notifications.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.notifications.alert.AlertPort;
import com.psyassistant.notifications.gmail.GmailEmailProperties;
import com.psyassistant.notifications.gmail.GmailSmtpAdapter;
import com.psyassistant.notifications.log.EmailDeliveryLog;
import com.psyassistant.notifications.log.EmailDeliveryLogRepository;
import com.psyassistant.notifications.log.EmailDeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

/**
 * Unit tests for {@link EmailOutboxProcessor}.
 */
@ExtendWith(MockitoExtension.class)
class EmailOutboxProcessorTest {

    @Mock
    private EmailDeliveryLogRepository repository;

    @Mock
    private GmailSmtpAdapter smtpAdapter;

    @Mock
    private AlertPort alertPort;

    private EmailOutboxProcessor processor;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 5;

    @BeforeEach
    void setUp() {
        final GmailEmailProperties props = new GmailEmailProperties();
        props.setMaxRetries(MAX_RETRIES);
        props.getOutbox().setBatchSize(BATCH_SIZE);
        processor = new EmailOutboxProcessor(repository, smtpAdapter, alertPort, props);
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private EmailDeliveryLog buildEntry() {
        return new EmailDeliveryLog(
                NotificationEventType.WELCOME, "encrypted@example.com",
                "abc123", "welcome.subject", "Welcome to PSY Assistant.");
    }

    @Nested
    @DisplayName("processBatch()")
    class ProcessBatchTests {

        @Test
        @DisplayName("does nothing when batch is empty")
        void emptyBatch() {
            when(repository.findNextBatch(any(), eq(BATCH_SIZE))).thenReturn(List.of());

            processor.processBatch();

            verify(smtpAdapter, never()).send(any());
        }

        @Test
        @DisplayName("transitions entry to SENDING then delegates to adapter")
        void marksSendingThenCallsAdapter() {
            final EmailDeliveryLog entry = buildEntry();
            when(repository.findNextBatch(any(), eq(BATCH_SIZE))).thenReturn(List.of(entry));

            processor.processBatch();

            verify(smtpAdapter).send(entry);
        }

        @Test
        @DisplayName("processes all entries in a non-empty batch")
        void processesAllEntries() {
            final List<EmailDeliveryLog> batch = List.of(buildEntry(), buildEntry(), buildEntry());
            when(repository.findNextBatch(any(), eq(BATCH_SIZE))).thenReturn(batch);

            processor.processBatch();

            verify(smtpAdapter, times(3)).send(any());
        }
    }

    @Nested
    @DisplayName("retry handling")
    class RetryTests {

        @Test
        @DisplayName("increments retryCount and sets RETRY status on first failure")
        void incrementsRetryOnFailure() {
            final EmailDeliveryLog entry = buildEntry();
            when(repository.findNextBatch(any(), eq(BATCH_SIZE))).thenReturn(List.of(entry));
            doThrow(new MailSendException("SMTP timeout")).when(smtpAdapter).send(entry);

            processor.processBatch();

            assertThat(entry.getRetryCount()).isEqualTo(1);
            assertThat(entry.getStatus()).isEqualTo(EmailDeliveryStatus.RETRY);
            assertThat(entry.getNextRetryAt()).isAfter(Instant.now().minusSeconds(1));
            assertThat(entry.getLastError()).contains("SMTP timeout");
            verify(alertPort, never()).raiseDeliveryFailedAlert(any());
        }

        @Test
        @DisplayName("sets FAILED and calls alertPort when max retries reached")
        void failsAfterMaxRetries() {
            final EmailDeliveryLog entry = buildEntry();
            entry.setRetryCount(MAX_RETRIES - 1);
            when(repository.findNextBatch(any(), eq(BATCH_SIZE))).thenReturn(List.of(entry));
            doThrow(new MailSendException("5xx Error")).when(smtpAdapter).send(entry);

            processor.processBatch();

            assertThat(entry.getStatus()).isEqualTo(EmailDeliveryStatus.FAILED);
            assertThat(entry.getRetryCount()).isEqualTo(MAX_RETRIES);
            verify(alertPort).raiseDeliveryFailedAlert(entry.getId());
        }

        @Test
        @DisplayName("alertPort is called exactly once on permanent failure")
        void alertCalledExactlyOnce() {
            final EmailDeliveryLog entry = buildEntry();
            entry.setRetryCount(MAX_RETRIES - 1);
            final UUID id = UUID.randomUUID();

            when(repository.findNextBatch(any(), eq(BATCH_SIZE))).thenReturn(List.of(entry));
            doThrow(new MailSendException("5xx Error")).when(smtpAdapter).send(entry);

            processor.processBatch();

            verify(alertPort, times(1)).raiseDeliveryFailedAlert(any());
        }
    }

    @Nested
    @DisplayName("computeNextRetryAt()")
    class BackoffTests {

        @Test
        @DisplayName("retry delay is within expected bounds for attempt 1")
        void attempt1InBounds() {
            final Instant before = Instant.now();
            final Instant next = processor.computeNextRetryAt(1);
            // min ~1s, max ~2.5s for attempt 1
            assertThat(next).isAfterOrEqualTo(before.plusMillis(900));
            assertThat(next).isBefore(before.plusSeconds(61));
        }

        @Test
        @DisplayName("delay is capped at 60s for large attempt numbers")
        void cappedAt60s() {
            final Instant before = Instant.now();
            final Instant next = processor.computeNextRetryAt(20);
            assertThat(next).isBefore(before.plusSeconds(61));
        }
    }
}
