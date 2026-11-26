package com.psyassistant.therapists.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRole;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class SingleTherapistProfileProvisionerTest {

    private TherapistProfileRepository repo;
    private SingleTherapistProfileProvisioner provisioner;
    private User user;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        repo = mock(TherapistProfileRepository.class);
        provisioner = new SingleTherapistProfileProvisioner(repo);
        orgId = UUID.randomUUID();
        user = new User("owner@example.com", "hash", EnumSet.of(UserRole.THERAPIST), true);
        user.setOrgId(orgId);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        when(repo.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(repo.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.empty());
        when(repo.save(any(TherapistProfile.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createsActiveLinkedProfileInUsersOrganization() {
        provisioner.ensureProfile(user);

        ArgumentCaptor<TherapistProfile> captor = ArgumentCaptor.forClass(TherapistProfile.class);
        verify(repo).save(captor.capture());
        TherapistProfile p = captor.getValue();
        assertThat(p.getEmail()).isEqualTo("owner@example.com");
        assertThat(p.getName()).isEqualTo("owner@example.com");
        assertThat(p.getOrgId()).isEqualTo(orgId);
        assertThat(p.getUserId()).isEqualTo(user.getId());
        assertThat(p.getActive()).isTrue();
    }

    @Test
    void doesNotCreateDuplicateWhenProfileAlreadyExists() {
        TherapistProfile existing = new TherapistProfile("owner@example.com", "Owner", null);
        when(repo.findByUserId(user.getId())).thenReturn(Optional.of(existing));

        assertThat(provisioner.ensureProfile(user)).isSameAs(existing);
        verify(repo, never()).save(any());
    }
}
