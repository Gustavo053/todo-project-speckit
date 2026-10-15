package com.todoproject.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todoproject.application.port.in.BoardSnapshot;
import com.todoproject.application.port.in.ViewBoardQuery;
import com.todoproject.domain.model.BoardStage;
import java.util.Arrays;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Covers FR-001, FR-003, FR-004: the board always shows all four columns, in order, with counts. */
@WebMvcTest(com.todoproject.adapter.in.web.BoardController.class)
@org.springframework.context.annotation.Import(com.todoproject.config.SecurityConfig.class)
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewBoardQuery viewBoardQuery;

    @Test
    void rendersAllFourEmptyColumnsInOrderWithCounts() throws Exception {
        List<BoardSnapshot.Column> columns = Arrays.stream(BoardStage.values())
                .map(stage -> new BoardSnapshot.Column(stage, List.of()))
                .toList();
        org.mockito.BDDMockito.given(viewBoardQuery.load()).willReturn(new BoardSnapshot(columns, 0, 500));

        MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

        Document document = Jsoup.parse(result.getResponse().getContentAsString());
        List<Element> columnElements = document.select(".column");
        assertThat(columnElements).hasSize(4);

        List<String> counts = columnElements.stream()
                .map(el -> el.select(".column__count").text())
                .toList();
        assertThat(counts).containsExactly("0", "0", "0", "0");

        assertThat(document.select(".column__empty")).hasSize(4);
    }
}
