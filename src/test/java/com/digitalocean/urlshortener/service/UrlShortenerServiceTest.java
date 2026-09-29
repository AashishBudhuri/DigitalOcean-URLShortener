package com.digitalocean.urlshortener.service;

import com.digitalocean.urlshortener.model.Url;
import com.digitalocean.urlshortener.store.UrlCache;
import com.digitalocean.urlshortener.store.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    private static final long DEFAULT_TTL = 86400L;

    @Mock
    private UrlRepository repository;

    @Mock
    private UrlCache cache;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(repository, cache, DEFAULT_TTL);
    }

    @Test
    void create_withCustomAlias_savesUrl() {
        when(repository.existsById("my-link")).thenReturn(false);
        when(repository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Url saved = service.create("https://example.com/path", "my-link");

        assertThat(saved.getAlias()).isEqualTo("my-link");
        assertThat(saved.getLongUrl()).isEqualTo("https://example.com/path");
        assertThat(saved.getTtl()).isEqualTo(DEFAULT_TTL);
        assertThat(saved.getTimeCreated()).isNotNull();

        ArgumentCaptor<Url> captor = ArgumentCaptor.forClass(Url.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAlias()).isEqualTo("my-link");
    }

    @Test
    void create_withoutAlias_generatesAndSavesAlias() {
        when(repository.existsById(any())).thenReturn(false);
        when(repository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Url saved = service.create("https://example.com/path", null);

        assertThat(saved.getAlias()).isNotBlank();
        assertThat(saved.getAlias()).matches("[0-9A-Za-z]+");
        verify(repository).save(any(Url.class));
    }

    @Test
    void create_whenAliasExists_throwsConflict() {
        when(repository.existsById("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.create("https://example.com", "taken"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(statusEx.getReason()).isEqualTo("Alias already taken");
                });

        verify(repository, never()).save(any());
    }

    @Test
    void resolve_whenCached_returnsCachedUrlWithoutHittingDb() {
        when(cache.get("abc")).thenReturn(Optional.of("https://cached.example"));

        Optional<String> result = service.resolve("abc");

        assertThat(result).contains("https://cached.example");
        verify(repository, never()).findById(any());
    }

    @Test
    void resolve_whenDbHit_cachesAndReturnsLongUrl() {
        Url url = new Url("abc", "https://db.example", Instant.now(), DEFAULT_TTL);
        when(cache.get("abc")).thenReturn(Optional.empty());
        when(repository.findById("abc")).thenReturn(Optional.of(url));

        Optional<String> result = service.resolve("abc");

        assertThat(result).contains("https://db.example");
        verify(cache).put(eq("abc"), eq("https://db.example"));
    }

    @Test
    void resolve_whenExpired_returnsEmpty() {
        Url expired = new Url("abc", "https://old.example", Instant.now().minusSeconds(100_000), 60L);
        when(cache.get("abc")).thenReturn(Optional.empty());
        when(repository.findById("abc")).thenReturn(Optional.of(expired));

        Optional<String> result = service.resolve("abc");

        assertThat(result).isEmpty();
        verify(cache, never()).put(any(), any());
    }

    @Test
    void resolve_whenMissing_returnsEmpty() {
        when(cache.get("missing")).thenReturn(Optional.empty());
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThat(service.resolve("missing")).isEmpty();
    }
}
