package com.psyassistant.admin;

import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import com.psyassistant.users.UserSpecification;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-organization user queries for the global administrator.
 */
@Service
public class GlobalAdminUserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Constructs the service.
     *
     * @param userRepository         user repository
     * @param organizationRepository organization repository
     */
    public GlobalAdminUserService(
            final UserRepository userRepository,
            final OrganizationRepository organizationRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    /**
     * Lists users of all organizations with optional filters.
     *
     * @param role     optional role filter
     * @param active   optional active-status filter
     * @param search   optional case-insensitive email/full-name substring
     * @param pageable page request
     * @return page of users enriched with organization info
     */
    @Transactional(readOnly = true)
    public GlobalUserPageResponse listUsers(
            final UserRole role, final Boolean active, final String search, final Pageable pageable) {
        UserRole normalizedRole = role == null ? null : role.canonical();
        Page<User> page = userRepository.findAll(
                UserSpecification.withFilters(normalizedRole, active, search), pageable);

        List<UUID> orgIds = page.getContent().stream()
                .map(User::getOrgId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Organization> orgs = organizationRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(Organization::getId, Function.identity()));

        List<GlobalUserSummaryDto> content = page.getContent().stream()
                .map(u -> GlobalUserSummaryDto.from(u, orgs.get(u.getOrgId())))
                .toList();
        return new GlobalUserPageResponse(
                content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }
}
