package com.psyassistant.notifications.template;

/**
 * Thrown when an update or delete is attempted on an ACTIVE template.
 * Templates must be deactivated before being modified or deleted.
 */
public class TemplateNotInactiveException extends RuntimeException {

    /**
     * Constructs the exception with a message referencing the template id.
     *
     * @param id the template id
     */
    public TemplateNotInactiveException(final java.util.UUID id) {
        super("Template " + id + " must be INACTIVE before it can be modified or deleted");
    }
}
