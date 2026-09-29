package com.digitalocean.urlshortener.store;

import java.util.Optional;

public interface UrlCache {

    Optional<String> get(String alias);

    void put(String alias, String longUrl);
}
