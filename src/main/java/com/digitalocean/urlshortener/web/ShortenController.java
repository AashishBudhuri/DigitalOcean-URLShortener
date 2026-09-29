package com.digitalocean.urlshortener.web;

import com.digitalocean.urlshortener.model.Url;
import com.digitalocean.urlshortener.service.UrlShortenerService;
import com.digitalocean.urlshortener.web.dto.ShortenRequest;
import com.digitalocean.urlshortener.web.dto.ShortenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ShortenController {

    private final UrlShortenerService service;

    public ShortenController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest
    ) {
        Url url = service.create(request.longUrl(), request.alias());
        String shortUrl = buildShortUrl(httpRequest, url.getAlias());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ShortenResponse(shortUrl, url.getAlias(), url.getLongUrl()));
    }

    private static String buildShortUrl(HttpServletRequest request, String alias) {
        String base = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        return base + "/" + alias;
    }
}
