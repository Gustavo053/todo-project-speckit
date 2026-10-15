package com.todoproject.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.todoproject.adapter.in.web.BoardController;
import com.todoproject.application.port.in.BoardSnapshot;
import com.todoproject.application.port.in.ViewBoardQuery;
import com.todoproject.domain.model.BoardStage;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers FR-019, FR-020a and research R10: no cookie renders no {@code data-theme} attribute so
 * CSS {@code prefers-color-scheme} stays in charge; an explicit choice renders the matching
 * attribute; the control's accessible name states the current and next state.
 */
@WebMvcTest(BoardController.class)
@Import(com.todoproject.config.SecurityConfig.class)
class ThemeRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewBoardQuery viewBoardQuery;

    @Test
    void noCookieRendersNoDataThemeAttribute() throws Exception {
        stubEmptyBoard();

        MvcResult result = mockMvc.perform(get("/")).andReturn();
        Document document = Jsoup.parse(result.getResponse().getContentAsString());

        assertThat(document.selectFirst("html").hasAttr("data-theme")).isFalse();
    }

    @Test
    void lightPreferenceRendersTheLightAttribute() throws Exception {
        stubEmptyBoard();

        MvcResult result = mockMvc.perform(get("/").cookie(new Cookie("appearance", "LIGHT")))
                .andReturn();
        Document document = Jsoup.parse(result.getResponse().getContentAsString());

        assertThat(document.selectFirst("html").attr("data-theme")).isEqualTo("light");
    }

    @Test
    void darkPreferenceRendersTheDarkAttribute() throws Exception {
        stubEmptyBoard();

        MvcResult result = mockMvc.perform(get("/").cookie(new Cookie("appearance", "DARK")))
                .andReturn();
        Document document = Jsoup.parse(result.getResponse().getContentAsString());

        assertThat(document.selectFirst("html").attr("data-theme")).isEqualTo("dark");
    }

    @Test
    void theControlHasAnAccessibleNameStatingCurrentAndNextState() throws Exception {
        stubEmptyBoard();

        MvcResult result = mockMvc.perform(get("/")).andReturn();
        Document document = Jsoup.parse(result.getResponse().getContentAsString());

        String ariaLabel = document.selectFirst(".theme-form__button").attr("aria-label");
        assertThat(ariaLabel).isNotBlank();
    }

    private void stubEmptyBoard() {
        List<BoardSnapshot.Column> columns = java.util.Arrays.stream(BoardStage.values())
                .map(stage -> new BoardSnapshot.Column(stage, List.of()))
                .toList();
        org.mockito.BDDMockito.given(viewBoardQuery.load()).willReturn(new BoardSnapshot(columns, 0, 500));
    }
}
