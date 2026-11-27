package com.psyassistant.contacts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link PlatformContactsService}. */
@ExtendWith(MockitoExtension.class)
class PlatformContactsServiceTest {

    @Mock
    private PlatformContactsRepository repository;

    @InjectMocks
    private PlatformContactsService service;

    @Test
    void getReturnsEmptyValuesWhenNothingConfigured() {
        when(repository.findById(PlatformContacts.SINGLETON_ID)).thenReturn(Optional.empty());

        ContactsResponse result = service.get();

        assertThat(result.phone()).isNull();
        assertThat(result.email()).isNull();
        assertThat(result.telegramChannels()).isEmpty();
    }

    @Test
    void updateNormalisesHandlesDeduplicatesAndClearsBlanks() {
        when(repository.findById(PlatformContacts.SINGLETON_ID)).thenReturn(Optional.empty());
        when(repository.save(any(PlatformContacts.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactsResponse result = service.update(new ContactsRequest(
                " +380 44 123 45 67 ", "  ", List.of("@psy_help", "https://t.me/psy_help", " ")));

        assertThat(result.phone()).isEqualTo("+380 44 123 45 67");
        assertThat(result.email()).isNull();
        assertThat(result.telegramChannels()).containsExactly("https://t.me/psy_help");
    }

    @Test
    void updateWithNullChannelsStoresNone() {
        when(repository.findById(PlatformContacts.SINGLETON_ID))
                .thenReturn(Optional.of(new PlatformContacts("1", "a@b.co", "https://t.me/old_channel")));
        when(repository.save(any(PlatformContacts.class))).thenAnswer(inv -> inv.getArgument(0));

        ContactsResponse result = service.update(new ContactsRequest(null, "a@b.co", null));

        assertThat(result.telegramChannels()).isEmpty();
    }
}
