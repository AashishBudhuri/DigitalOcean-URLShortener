package com.digitalocean.urlshortener.web;

import com.digitalocean.urlshortener.model.Url;
import com.digitalocean.urlshortener.service.UrlShortenerService;
import com.digitalocean.urlshortener.web.dto.CreateURLResult;
import com.digitalocean.urlshortener.web.dto.ShortenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ShortenController {

    private final UrlShortenerService service;

    public ShortenController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public CreateURLResult shorten(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Url url = service.create(request.longUrl(), request.alias());
            String shortUrl = buildShortUrl(httpRequest, url.getAlias());
            return CreateURLResult.success(shortUrl, url.getAlias());
        } catch (ResponseStatusException ex) {
            String message = ex.getReason() != null ? ex.getReason() : "Request failed";
            return CreateURLResult.failure(message);
        }
    }

    private static String buildShortUrl(HttpServletRequest request, String alias) {
        String base = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        return base + "/" + alias;
    }
}
