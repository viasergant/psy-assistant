package com.psyassistant.billing.catalog;

/**
 * Thrown when an attempt is made to create or rename a service using a name+category
 * combination that already exists in the catalog.
 *
 * <p>Maps to HTTP 409 Conflict via {@link com.psyassistant.common.exception.GlobalExceptionHandler}.
 */
public class DuplicateServiceException extends RuntimeException {

    public DuplicateServiceException(final String name, final String category) {
        super("A service with name '" + name + "' already exists in category '" + category + "'");
    }
}
