package com.digitalocean.urlshortener.web;

import com.digitalocean.urlshortener.model.Url;
import com.digitalocean.urlshortener.service.UrlShortenerService;
import com.digitalocean.urlshortener.web.dto.CreateURLResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShortenController.class)
class ShortenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlShortenerService service;

    @Test
    void shorten_returnsSuccess() throws Exception {
        Url url = new Url("my-link", "https://example.com", Instant.now(), 86400L);
        when(service.create(eq("https://example.com"), eq("my-link"))).thenReturn(url);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl":"https://example.com","alias":"my-link"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CreateURLResponse.SUCCESS.name()))
                .andExpect(jsonPath("$.errorMessage").value(nullValue()))
                .andExpect(jsonPath("$.alias").value("my-link"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/my-link"));
    }

    @Test
    void shorten_whenAliasTaken_returnsFailure() throws Exception {
        when(service.create(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Alias already taken"));

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl":"https://example.com","alias":"taken"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CreateURLResponse.FAILURE.name()))
                .andExpect(jsonPath("$.errorMessage").value("Alias already taken"))
                .andExpect(jsonPath("$.shortUrl").value(nullValue()))
                .andExpect(jsonPath("$.alias").value(nullValue()));
    }

    @Test
    void shorten_withoutAlias_returnsSuccess() throws Exception {
        Url url = new Url("generated", "https://example.com", Instant.now(), 86400L);
        when(service.create(eq("https://example.com"), isNull())).thenReturn(url);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl":"https://example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CreateURLResponse.SUCCESS.name()))
                .andExpect(jsonPath("$.alias").value("generated"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/generated"));
    }
}
