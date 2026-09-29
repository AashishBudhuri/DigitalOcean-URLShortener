package com.digitalocean.urlshortener.web.dto;

public record CreateURLResult(
        CreateURLResponse status,
        String errorMessage,
        String shortUrl,
        String alias
) {

    public static CreateURLResult success(String shortUrl, String alias) {
        return new CreateURLResult(CreateURLResponse.SUCCESS, null, shortUrl, alias);
    }

    public static CreateURLResult failure(String errorMessage) {
        return new CreateURLResult(CreateURLResponse.FAILURE, errorMessage, null, null);
    }
}
