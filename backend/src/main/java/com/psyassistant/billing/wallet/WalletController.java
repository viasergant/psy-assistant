package com.psyassistant.billing.wallet;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for client credit wallets.
 *
 * <p>Currently exposes a read-only balance endpoint.
 * Wallet top-up via overpayment auto-credit is deferred to a future release.
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/wallet")
public class WalletController {

    private final ClientWalletRepository walletRepository;

    public WalletController(final ClientWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Returns the credit wallet balance for a client.
     * Returns 0.00 if no wallet exists yet.
     *
     * @param clientId client UUID from path
     * @return 200 with wallet balance
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('READ_INVOICES', 'MANAGE_PAYMENTS')")
    public ResponseEntity<WalletBalanceResponse> getWallet(@PathVariable final UUID clientId) {
        BigDecimal balance = walletRepository.findByClientId(clientId)
                .map(ClientWallet::getBalance)
                .orElse(BigDecimal.ZERO);
        return ResponseEntity.ok(new WalletBalanceResponse(clientId, balance));
    }

    /** Response record for wallet endpoint. */
    public record WalletBalanceResponse(UUID clientId, BigDecimal balance) { }
}
