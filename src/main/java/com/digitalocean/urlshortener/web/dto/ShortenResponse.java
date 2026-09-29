package com.digitalocean.urlshortener.web.dto;

public record ShortenResponse(String shortUrl, String alias, String longUrl) {
}
