package com.psyassistant.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.psyassistant.organization.Organization;
import com.psyassistant.organization.OrganizationRepository;
import com.psyassistant.organization.OrganizationType;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

/** Unit tests for {@link GlobalAdminUserService}. */
class GlobalAdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final GlobalAdminUserService service =
            new GlobalAdminUserService(userRepository, organizationRepository);

    @Test
    @SuppressWarnings("unchecked")
    void enrichesUsersWithOrganizationAndToleratesMissingOrg() {
        UUID orgId = UUID.randomUUID();
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(orgId);
        when(org.getName()).thenReturn("Acme Practice");
        when(org.getType()).thenReturn(OrganizationType.SOLO);

        User withOrg = mock(User.class);
        when(withOrg.getOrgId()).thenReturn(orgId);
        when(withOrg.getEmail()).thenReturn("a@example.com");
        User withoutOrg = mock(User.class);
        when(withoutOrg.getOrgId()).thenReturn(null);
        when(withoutOrg.getEmail()).thenReturn("b@example.com");

        PageRequest pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(withOrg, withoutOrg), pageable, 2));
        when(organizationRepository.findAllById(anyIterable())).thenReturn(List.of(org));

        GlobalUserPageResponse result = service.listUsers(null, null, null, pageable);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content().get(0).orgName()).isEqualTo("Acme Practice");
        assertThat(result.content().get(0).orgType()).isEqualTo(OrganizationType.SOLO);
        assertThat(result.content().get(1).orgName()).isNull();
    }
}
