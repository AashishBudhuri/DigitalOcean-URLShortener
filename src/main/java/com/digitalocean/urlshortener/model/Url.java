package com.digitalocean.urlshortener.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "urls")
public class Url {

    @Id
    @Column(nullable = false, updatable = false, length = 64)
    private String alias;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "time_created", nullable = false, updatable = false)
    private Instant timeCreated;

    /** Time-to-live in seconds; null means the URL never expires. */
    @Column(name = "ttl")
    private Long ttl;

    protected Url() {
    }

    public Url(String alias, String longUrl, Instant timeCreated, Long ttl) {
        this.alias = alias;
        this.longUrl = longUrl;
        this.timeCreated = timeCreated;
        this.ttl = ttl;
    }

    public String getAlias() {
        return alias;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public Instant getTimeCreated() {
        return timeCreated;
    }

    public Long getTtl() {
        return ttl;
    }

    public boolean isExpired(Instant now) {
        if (ttl == null) {
            return false;
        }
        return now.isAfter(timeCreated.plusSeconds(ttl));
    }
}
