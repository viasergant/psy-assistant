package com.psyassistant.crm.leads;

import java.util.Collections;
import java.util.Set;

/**
 * Finite-state machine for the lead lifecycle.
 *
 * <p>Each status carries the set of statuses it may legally transition to.
 * Terminal statuses (CONVERTED and INACTIVE) have no allowed transitions.
 */
public enum LeadStatus {

    /** Initial status for a newly created lead. */
    NEW(Set.of(LeadStatus.Values.CONTACTED, LeadStatus.Values.INACTIVE)),

    /** The lead has been reached out to. */
    CONTACTED(Set.of(LeadStatus.Values.QUALIFIED, LeadStatus.Values.INACTIVE)),

    /** The lead has been assessed as a potential client. */
    QUALIFIED(Set.of(LeadStatus.Values.CONVERTED, LeadStatus.Values.INACTIVE)),

    /** Terminal: the lead was converted to a client. */
    CONVERTED(Set.of()),

    /** Terminal: the lead is archived (inactive). */
    INACTIVE(Set.of());

    private final Set<String> allowedTransitionNames;

    LeadStatus(final Set<String> allowed) {
        this.allowedTransitionNames = Collections.unmodifiableSet(allowed);
    }

    /**
     * Returns true if transitioning to the given target status is permitted.
     *
     * @param target the desired next status
     * @return true when the transition is valid
     */
    public boolean canTransitionTo(final LeadStatus target) {
        return allowedTransitionNames.contains(target.name());
    }

    /**
     * String constants used to initialise the enum sets before enum instances are fully built.
     * This is required because Java does not allow referencing enum values in their own
     * constructor before all instances are created.
     */
    private static final class Values {
        static final String CONTACTED = "CONTACTED";
        static final String QUALIFIED  = "QUALIFIED";
        static final String CONVERTED  = "CONVERTED";
        static final String INACTIVE   = "INACTIVE";

        private Values() {
        }
    }
}
