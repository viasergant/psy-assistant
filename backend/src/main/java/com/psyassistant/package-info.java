/**
 * Root package for the psy-assistant backend application.
 *
 * <p>The {@code orgFilter} Hibernate filter is declared here once at the package level
 * so that it is registered exactly once with the persistence unit. Individual entity
 * classes apply the filter via {@link org.hibernate.annotations.Filter @Filter} only.
 */
@FilterDef(
    name = "orgFilter",
    parameters = @ParamDef(name = "orgId", type = java.util.UUID.class)
)
package com.psyassistant;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
