package com.todoproject.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todoproject.adapter.in.web.BoardController;
import com.todoproject.application.port.in.BoardSnapshot;
import com.todoproject.application.port.in.ViewBoardQuery;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;
import java.util.ArrayList;
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
 * Covers FR-023 and SC-009: card content typed by a person is never interpreted as markup. Jsoup
 * confirms the payload produced no element -- it is literal text, not escaped text that merely
 * looks safe in the raw HTML string.
 */
@WebMvcTest(BoardController.class)
@Import(com.todoproject.config.SecurityConfig.class)
class EscapingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewBoardQuery viewBoardQuery;

    @Test
    void scriptTagInTitleAndMarkupInNoteRenderAsLiteralText() throws Exception {
        TaskCard malicious = TaskCard.create(
                CardTitle.of("tarefa normal"), // placeholder; real value set via reconstitute below
                CardNote.empty(),
                BoardStage.BACKLOG,
                Instant.now());

        // CardTitle's own allowlist would reject angle brackets as punctuation? No -- '<' and '>'
        // are category Sm/Po and ARE permitted by the allowlist (research R5 permits punctuation
        // and symbols); the policy blocks invisible/control categories, not markup-looking
        // characters. Escaping at render time (not input rejection) is what stops it executing.
        TaskCard card = TaskCard.reconstitute(
                malicious.id(),
                CardTitle.fromStored("<script>alert(1)</script>"),
                CardNote.fromStored("<img src=x onerror=alert(1)>"),
                BoardStage.BACKLOG,
                Instant.now(),
                Instant.now(),
                0L);

        List<BoardSnapshot.Column> columns = new ArrayList<>();
        for (BoardStage stage : BoardStage.values()) {
            columns.add(new BoardSnapshot.Column(stage, stage == BoardStage.BACKLOG ? List.of(card) : List.of()));
        }
        org.mockito.BDDMockito.given(viewBoardQuery.load()).willReturn(new BoardSnapshot(columns, 1, 500));

        MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();

        Document document = Jsoup.parse(body);
        // No <script> element and no <img> element were created from the card's content.
        assertThat(document.select("script")).isEmpty();
        assertThat(document.select(".card img")).isEmpty();

        // The raw response contains the escaped forms, not the live tags.
        assertThat(body).doesNotContain("<script>alert(1)</script>");
        assertThat(body).contains("&lt;script&gt;");

        // And the text content, once parsed, is exactly what was typed.
        assertThat(document.select(".card__title").text()).isEqualTo("<script>alert(1)</script>");
    }
}
