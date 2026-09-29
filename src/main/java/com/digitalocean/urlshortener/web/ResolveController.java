package com.digitalocean.urlshortener.web;

import com.digitalocean.urlshortener.service.UrlShortenerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ResolveController {

    private final UrlShortenerService service;

    public ResolveController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/{alias:[A-Za-z0-9_-]+}")
    public ResponseEntity<Void> resolve(@PathVariable String alias) {
        String longUrl = service.resolve(alias)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short URL not found"));

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, longUrl)
                .build();
    }
}
