package com.todoproject.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todoproject.adapter.in.web.ThemeController;
import com.todoproject.application.port.in.ViewBoardQuery;
import com.todoproject.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Covers FR-019, FR-020 and the cookie properties in research R10. */
@WebMvcTest(ThemeController.class)
@Import(SecurityConfig.class)
class ThemeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private ViewBoardQuery viewBoardQuery;

    @Test
    void acceptsEachOfTheThreeStates() throws Exception {
        for (String state : List.of("SYSTEM", "LIGHT", "DARK")) {
            mockMvc.perform(post("/theme")
                            .param("preference", state)
                            .param("redirectTo", "/")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Test
    void theCookieIsFunctionalOnlyWithNoIdentifier() throws Exception {
        MvcResult result = mockMvc.perform(post("/theme")
                        .param("preference", "DARK")
                        .param("redirectTo", "/")
                        .with(csrf()))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("appearance=DARK");
        assertThat(setCookie).containsIgnoringCase("HttpOnly");
        assertThat(setCookie).containsIgnoringCase("SameSite=Lax");
        assertThat(setCookie)
                .containsIgnoringCase(
                        "Max-Age=" + java.time.Duration.ofDays(365).toSeconds());
    }

    @Test
    void redirectToOutsideTheApplicationFallsBackToTheBoard() throws Exception {
        mockMvc.perform(post("/theme")
                        .param("preference", "LIGHT")
                        .param("redirectTo", "https://evil.example/")
                        .with(csrf()))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void protocolRelativeRedirectAlsoFallsBackToTheBoard() throws Exception {
        mockMvc.perform(post("/theme")
                        .param("preference", "LIGHT")
                        .param("redirectTo", "//evil.example/")
                        .with(csrf()))
                .andExpect(redirectedUrl("/"));
    }
}
