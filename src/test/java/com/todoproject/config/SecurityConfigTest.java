package com.todoproject.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todoproject.application.port.in.BoardSnapshot;
import com.todoproject.application.port.in.CreateCardUseCase;
import com.todoproject.application.port.in.CreateCardUseCase.CreateCardCommand;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers research R6: CSRF is enforced on HTML form routes, exempt on {@code /api/**}, and every
 * response carries frame denial and a Content-Security-Policy with no inline script.
 *
 * <p>{@code SecurityConfig} is imported explicitly: {@code @WebMvcTest} only auto-detects
 * controller-layer bean types, not an arbitrary {@code @Configuration} class, so the real filter
 * chain would not otherwise be on the test context.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.todoproject.application.port.in.ViewBoardQuery viewBoardQuery;

    @MockBean
    private com.todoproject.application.port.in.ViewCardQuery viewCardQuery;

    @MockBean
    private CreateCardUseCase createCardUseCase;

    @MockBean
    private com.todoproject.application.port.in.EditCardUseCase editCardUseCase;

    @MockBean
    private com.todoproject.application.port.in.MoveCardUseCase moveCardUseCase;

    @MockBean
    private com.todoproject.application.port.in.DeleteCardUseCase deleteCardUseCase;

    @BeforeEach
    void stubEmptyBoard() {
        List<BoardSnapshot.Column> columns = java.util.Arrays.stream(BoardStage.values())
                .map(stage -> new BoardSnapshot.Column(stage, List.of()))
                .toList();
        org.mockito.BDDMockito.given(viewBoardQuery.load()).willReturn(new BoardSnapshot(columns, 0, 500));
    }

    @Test
    void htmlFormPostWithoutCsrfTokenIsRefused() throws Exception {
        mockMvc.perform(post("/cards").param("title", "Tarefa")).andExpect(status().isForbidden());
    }

    @Test
    void htmlFormPostWithCsrfTokenSucceeds() throws Exception {
        org.mockito.BDDMockito.given(createCardUseCase.create(org.mockito.ArgumentMatchers.any(CreateCardCommand.class)))
                .willReturn(TaskCard.create(
                        CardTitle.of("Tarefa"), CardNote.empty(), BoardStage.BACKLOG, Instant.now()));

        mockMvc.perform(post("/cards")
                        .param("title", "Tarefa")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void apiWriteSucceedsWithoutACsrfToken() throws Exception {
        org.mockito.BDDMockito.given(createCardUseCase.create(org.mockito.ArgumentMatchers.any(CreateCardCommand.class)))
                .willReturn(TaskCard.create(
                        CardTitle.of("Tarefa"), CardNote.empty(), BoardStage.BACKLOG, Instant.now()));

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarefa\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void everyResponseDeniesFraming() throws Exception {
        mockMvc.perform(get("/")).andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void everyResponseCarriesAContentSecurityPolicyWithNoInlineScript() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(result -> {
                    String csp = result.getResponse().getHeader("Content-Security-Policy");
                    org.assertj.core.api.Assertions.assertThat(csp).isNotNull();
                    org.assertj.core.api.Assertions.assertThat(csp).contains("default-src 'self'");
                    org.assertj.core.api.Assertions.assertThat(csp).doesNotContain("unsafe-inline");
                });
    }
}
