package com.digitalocean.urlshortener.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ShortenRequest(
        @NotBlank(message = "Long URL is required")
        String longUrl,
        String alias
) {
}
