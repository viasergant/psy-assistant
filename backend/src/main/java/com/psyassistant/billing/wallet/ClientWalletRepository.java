package com.psyassistant.billing.wallet;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ClientWallet} entities. */
public interface ClientWalletRepository extends JpaRepository<ClientWallet, UUID> {

    Optional<ClientWallet> findByClientId(UUID clientId);
}
