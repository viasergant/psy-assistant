package com.psyassistant.notifications.telegram;

import com.psyassistant.common.audit.BaseEntity;
import com.psyassistant.common.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Per-user Telegram link and notification preferences.
 *
 * <p>The chat id is encrypted at rest; {@code chatIdHash} (SHA-256) enforces that one Telegram
 * chat is linked to at most one user. <strong>Never log the chat id.</strong>
 */
@Filter(name = "orgFilter", condition = "org_id = :orgId")
@Entity
@Table(name = "therapist_notification_settings")
public class TherapistNotificationSettings extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "chat_id_encrypted")
    private String chatId;

    @Column(name = "chat_id_hash", length = 64, unique = true)
    private String chatIdHash;

    @Column(name = "linked_at")
    private Instant linkedAt;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(name = "language", nullable = false, length = 5)
    private String language = "en";

    @Column(name = "digest_enabled", nullable = false)
    private boolean digestEnabled = true;

    @Column(name = "digest_time", nullable = false)
    private LocalTime digestTime = LocalTime.of(7, 30);

    /** Bit mask, bit 0 = Monday ... bit 6 = Sunday. */
    @Column(name = "digest_days", nullable = false)
    private int digestDays = 127;

    @Column(name = "skip_empty_digest", nullable = false)
    private boolean skipEmptyDigest = true;

    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled = true;

    @Column(name = "reminder_minutes_before", nullable = false)
    private int reminderMinutesBefore = 60;

    @Column(name = "include_client_names", nullable = false)
    private boolean includeClientNames;

    protected TherapistNotificationSettings() {
    }

    /**
     * Creates default (unlinked) settings for a user.
     *
     * @param orgId  owning organization
     * @param userId owning user
     */
    public TherapistNotificationSettings(final UUID orgId, final UUID userId) {
        this.orgId = orgId;
        this.userId = userId;
    }

    /**
     * Returns whether a Telegram chat is linked.
     *
     * @return true when linked
     */
    public boolean isLinked() {
        return chatId != null;
    }

    /**
     * Binds a Telegram chat to this user.
     *
     * @param chatId     plaintext chat id
     * @param chatIdHash SHA-256 hex of the chat id
     * @param now        link time
     */
    public void link(final String chatId, final String chatIdHash, final Instant now) {
        this.chatId = chatId;
        this.chatIdHash = chatIdHash;
        this.linkedAt = now;
    }

    /** Removes the Telegram link. */
    public void unlink() {
        this.chatId = null;
        this.chatIdHash = null;
        this.linkedAt = null;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getChatId() {
        return chatId;
    }

    public String getChatIdHash() {
        return chatIdHash;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(final String timezone) {
        this.timezone = timezone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public boolean isDigestEnabled() {
        return digestEnabled;
    }

    public void setDigestEnabled(final boolean digestEnabled) {
        this.digestEnabled = digestEnabled;
    }

    public LocalTime getDigestTime() {
        return digestTime;
    }

    public void setDigestTime(final LocalTime digestTime) {
        this.digestTime = digestTime;
    }

    public int getDigestDays() {
        return digestDays;
    }

    public void setDigestDays(final int digestDays) {
        this.digestDays = digestDays;
    }

    public boolean isSkipEmptyDigest() {
        return skipEmptyDigest;
    }

    public void setSkipEmptyDigest(final boolean skipEmptyDigest) {
        this.skipEmptyDigest = skipEmptyDigest;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(final boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public int getReminderMinutesBefore() {
        return reminderMinutesBefore;
    }

    public void setReminderMinutesBefore(final int reminderMinutesBefore) {
        this.reminderMinutesBefore = reminderMinutesBefore;
    }

    public boolean isIncludeClientNames() {
        return includeClientNames;
    }

    public void setIncludeClientNames(final boolean includeClientNames) {
        this.includeClientNames = includeClientNames;
    }
}
