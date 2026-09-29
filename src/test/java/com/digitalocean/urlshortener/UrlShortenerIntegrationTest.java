package com.digitalocean.urlshortener;

import com.digitalocean.urlshortener.web.dto.CreateURLResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void create_success_returnsAliasAndShortUrl() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl":"https://example.com/integ-success","alias":"integ-ok"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CreateURLResponse.SUCCESS.name()))
                .andExpect(jsonPath("$.errorMessage").value(nullValue()))
                .andExpect(jsonPath("$.alias").value("integ-ok"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/integ-ok"));
    }

    @Test
    void create_failure_whenAliasAlreadyTaken() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl":"https://example.com/first","alias":"integ-dup"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CreateURLResponse.SUCCESS.name()));

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl":"https://example.com/second","alias":"integ-dup"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CreateURLResponse.FAILURE.name()))
                .andExpect(jsonPath("$.errorMessage").value("Alias already taken"))
                .andExpect(jsonPath("$.shortUrl").value(nullValue()))
                .andExpect(jsonPath("$.alias").value(nullValue()));
    }

    @Test
    void get_redirectsToLongUrl() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl":"https://example.com/resolved","alias":"integ-get"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CreateURLResponse.SUCCESS.name()));

        mockMvc.perform(get("/integ-get"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/resolved"));
    }

    @Test
    void get_whenMissing_returnsNotFound() throws Exception {
        mockMvc.perform(get("/integ-missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withoutAlias_thenGet_worksEndToEnd() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longUrl":"https://example.com/generated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(CreateURLResponse.SUCCESS.name()))
                .andExpect(jsonPath("$.alias").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andReturn();

        String body = created.getResponse().getContentAsString();
        String alias = body.replaceAll("(?s).*\"alias\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertThat(alias).isNotBlank();

        mockMvc.perform(get("/" + alias))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/generated"));
    }
}
