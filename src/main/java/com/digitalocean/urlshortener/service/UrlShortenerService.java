package com.digitalocean.urlshortener.service;

import com.digitalocean.urlshortener.model.Url;
import com.digitalocean.urlshortener.store.UrlCache;
import com.digitalocean.urlshortener.store.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UrlShortenerService {

    private static final String BASE62 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final UrlRepository repository;
    private final UrlCache cache;
    private final Long defaultTtlSeconds;

    public UrlShortenerService(
            UrlRepository repository,
            UrlCache cache,
            @Value("${url.default-ttl-seconds:86400}") Long defaultTtlSeconds
    ) {
        this.repository = repository;
        this.cache = cache;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public synchronized Url create(String longUrl, String optionalAlias) {
        String alias = (optionalAlias == null || optionalAlias.isBlank())
                ? generateAlias()
                : optionalAlias.trim();

        if (repository.existsById(alias)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alias already taken");
        }

        Url url = new Url(alias, longUrl.trim(), Instant.now(), defaultTtlSeconds);
        return repository.save(url);
    }

    public Optional<String> resolve(String alias) {
        Optional<String> cached = cache.get(alias);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<Url> found = repository.findById(alias)
                .filter(url -> !url.isExpired(Instant.now()));

        found.ifPresent(url -> cache.put(url.getAlias(), url.getLongUrl()));
        return found.map(Url::getLongUrl);
    }

    private String generateAlias() {
        return toBase62(UUID.randomUUID());
    }

    private static String toBase62(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        BigInteger value = new BigInteger(1, buffer.array());

        if (value.equals(BigInteger.ZERO)) {
            return "0";
        }

        StringBuilder encoded = new StringBuilder();
        BigInteger base = BigInteger.valueOf(62);
        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = value.divideAndRemainder(base);
            encoded.append(BASE62.charAt(divRem[1].intValue()));
            value = divRem[0];
        }
        return encoded.reverse().toString();
    }
}
