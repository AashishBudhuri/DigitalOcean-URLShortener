package com.digitalocean.urlshortener.web;

import com.digitalocean.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResolveController.class)
class ResolveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlShortenerService service;

    @Test
    void resolve_whenFound_redirectsToLongUrl() throws Exception {
        when(service.resolve("my-link")).thenReturn(Optional.of("https://example.com/test"));

        mockMvc.perform(get("/my-link"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/test"));
    }

    @Test
    void resolve_whenMissing_returnsNotFound() throws Exception {
        when(service.resolve("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound());
    }
}
