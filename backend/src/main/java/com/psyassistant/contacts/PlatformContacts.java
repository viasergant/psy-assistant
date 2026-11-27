package com.psyassistant.contacts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Platform-wide contact details (single row, {@code id = 1}).
 *
 * <p>Global data shared by all organizations, so it has no {@code org_id} and no tenant filter.
 */
@Entity
@Table(name = "platform_contacts")
public class PlatformContacts {

    /** Fixed primary key of the single row. */
    public static final short SINGLETON_ID = 1;

    @Id
    private short id = SINGLETON_ID;

    @Column(length = 32)
    private String phone;

    @Column(length = 254)
    private String email;

    /** Telegram links separated by a newline. */
    @Column(name = "telegram_channels")
    private String telegramChannels;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PlatformContacts() { }

    /**
     * Creates the singleton row with the given values.
     *
     * @param phone            phone number, may be null
     * @param email            e-mail address, may be null
     * @param telegramChannels newline-separated Telegram links, may be null
     */
    public PlatformContacts(final String phone, final String email, final String telegramChannels) {
        update(phone, email, telegramChannels);
    }

    /**
     * Replaces all values and refreshes the update timestamp.
     *
     * @param newPhone            phone number, may be null
     * @param newEmail            e-mail address, may be null
     * @param newTelegramChannels newline-separated Telegram links, may be null
     */
    public final void update(final String newPhone, final String newEmail, final String newTelegramChannels) {
        this.phone = newPhone;
        this.email = newEmail;
        this.telegramChannels = newTelegramChannels;
        this.updatedAt = Instant.now();
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getTelegramChannels() {
        return telegramChannels;
    }
}
