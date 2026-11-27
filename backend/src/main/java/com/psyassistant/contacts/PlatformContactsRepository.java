package com.psyassistant.contacts;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for the single {@link PlatformContacts} row. */
public interface PlatformContactsRepository extends JpaRepository<PlatformContacts, Short> {
}
