package com.psyassistant.billing.wallet;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Client credit wallet — one row per client.
 *
 * <p>Created when a client overpays (future feature). Currently the balance stays at 0
 * because overpayments are rejected (PA-48 scope).
 */
@Entity
@Table(name = "client_wallets")
public class ClientWallet extends BaseEntity {

    @Column(name = "client_id", nullable = false, unique = true, updatable = false)
    private UUID clientId;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "updated_by")
    private String updatedBy;

    protected ClientWallet() { }

    public ClientWallet(final UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
