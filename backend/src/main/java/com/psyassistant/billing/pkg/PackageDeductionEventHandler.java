package com.psyassistant.billing.pkg;

import com.psyassistant.billing.catalog.ServiceType;
import com.psyassistant.sessions.event.SessionCompletedEvent;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link SessionCompletedEvent} by attempting to auto-deduct one session
 * from the client's oldest eligible active prepaid package.
 *
 * <p>The deduction participates in the same transaction as the session completion,
 * so a rollback of the completion also rolls back the deduction.
 *
 * <p>No deduction occurs when:
 * <ul>
 *   <li>The session has no session type</li>
 *   <li>The session type code has no billing {@link ServiceType} mapping</li>
 *   <li>No eligible active package exists for the client and resolved service type</li>
 * </ul>
 */
@Component
public class PackageDeductionEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PackageDeductionEventHandler.class);
    private static final String SYSTEM_ACTOR = "system:package-deduction";

    private final PackageService packageService;
    private final SessionTypeToServiceTypeMapper serviceTypeMapper;

    public PackageDeductionEventHandler(
            final PackageService packageService,
            final SessionTypeToServiceTypeMapper serviceTypeMapper) {
        this.packageService = packageService;
        this.serviceTypeMapper = serviceTypeMapper;
    }

    /**
     * Reacts to a completed session by deducting from the matching active package, if any.
     *
     * @param event the session-completed event
     */
    @EventListener
    @Transactional
    public void handleSessionCompleted(final SessionCompletedEvent event) {
        if (event.sessionType() == null) {
            LOG.debug("Session {} has no session type — skipping package deduction", event.sessionId());
            return;
        }

        final String code = event.sessionType().getCode();
        final Optional<ServiceType> serviceType = serviceTypeMapper.resolve(code);

        if (serviceType.isEmpty()) {
            LOG.debug("Session type '{}' has no billing mapping — skipping package deduction for session {}",
                    code, event.sessionId());
            return;
        }

        final PackageDeductionResult result = packageService.tryDeductSession(
                event.clientId(),
                serviceType.get(),
                event.sessionId(),
                event.therapistId(),
                SYSTEM_ACTOR
        );

        switch (result) {
            case PackageDeductionResult.Deducted d ->
                LOG.info("Auto-deducted 1 session from package {} for client {} (session {}): {} → {}",
                        d.packageInstanceId(), event.clientId(), event.sessionId(),
                        d.balanceBefore(), d.balanceAfter());
            case PackageDeductionResult.PackageExhausted e ->
                LOG.info("Package {} exhausted after auto-deduction for client {} (session {})",
                        e.packageInstanceId(), event.clientId(), event.sessionId());
            case PackageDeductionResult.NoEligiblePackage ignored ->
                LOG.debug("No eligible package for client {} with service type {} (session {})",
                        event.clientId(), serviceType.get(), event.sessionId());
        }
    }
}
