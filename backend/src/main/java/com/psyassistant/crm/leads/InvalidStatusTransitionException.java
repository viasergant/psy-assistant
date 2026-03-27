package com.psyassistant.crm.leads;

/**
 * Thrown when an attempt is made to transition a lead to a status that is not
 * reachable from the lead's current status.
 *
 * <p>Mapped to HTTP 422 Unprocessable Entity by the global exception handler.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    /**
     * Constructs the exception with a message describing the illegal transition.
     *
     * @param from   the lead's current status
     * @param to     the requested target status
     */
    public InvalidStatusTransitionException(final LeadStatus from, final LeadStatus to) {
        super("Cannot transition lead from " + from + " to " + to);
    }
}
