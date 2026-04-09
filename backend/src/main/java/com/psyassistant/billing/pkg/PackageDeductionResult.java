package com.psyassistant.billing.pkg;

import java.util.UUID;

/**
 * Sealed result type returned by {@link PackageService#tryDeductSession}.
 *
 * <p>Callers pattern-match on the concrete record type to decide the next action.
 */
public sealed interface PackageDeductionResult {

    /** Session was successfully deducted from the package. */
    record Deducted(UUID packageInstanceId, int balanceBefore, int balanceAfter)
            implements PackageDeductionResult { }

    /** The package exists but has just been exhausted (sessionsRemaining reached 0). */
    record PackageExhausted(UUID packageInstanceId)
            implements PackageDeductionResult { }

    /** No eligible active package was found for the client and service type. */
    record NoEligiblePackage()
            implements PackageDeductionResult { }
}
