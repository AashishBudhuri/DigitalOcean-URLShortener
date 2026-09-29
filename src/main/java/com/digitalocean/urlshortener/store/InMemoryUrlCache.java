package com.digitalocean.urlshortener.store;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stand-in cache. Replace with Redis (or similar) later.
 */
@Component
public class InMemoryUrlCache implements UrlCache {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String alias) {
        return Optional.ofNullable(cache.get(alias));
    }

    @Override
    public void put(String alias, String longUrl) {
        cache.put(alias, longUrl);
    }
}
