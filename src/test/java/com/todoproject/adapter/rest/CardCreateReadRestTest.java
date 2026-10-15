package com.todoproject.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todoproject.adapter.in.rest.BoardRestController;
import com.todoproject.adapter.in.rest.CardRestController;
import com.todoproject.application.port.in.BoardSnapshot;
import com.todoproject.application.port.in.CreateCardUseCase;
import com.todoproject.application.port.in.CreateCardUseCase.CreateCardCommand;
import com.todoproject.application.port.in.DeleteCardUseCase;
import com.todoproject.application.port.in.EditCardUseCase;
import com.todoproject.application.port.in.MoveCardUseCase;
import com.todoproject.application.port.in.ViewBoardQuery;
import com.todoproject.application.port.in.ViewCardQuery;
import com.todoproject.domain.error.BoardFullException;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Covers the REST surface against contracts/rest-api.yaml. No CSRF token is ever supplied here --
 * this surface is cookie-less and exempt (research R6). */
@WebMvcTest({BoardRestController.class, CardRestController.class})
class CardCreateReadRestTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewBoardQuery viewBoardQuery;

    @MockBean
    private ViewCardQuery viewCardQuery;

    @MockBean
    private CreateCardUseCase createCardUseCase;

    @MockBean
    private EditCardUseCase editCardUseCase;

    @MockBean
    private MoveCardUseCase moveCardUseCase;

    @MockBean
    private DeleteCardUseCase deleteCardUseCase;

    @Test
    void createReturns201WithLocationAndETag() throws Exception {
        TaskCard created =
                TaskCard.create(CardTitle.of("Comprar pão"), CardNote.empty(), BoardStage.BACKLOG, Instant.now());
        BDDMockito.given(createCardUseCase.create(any(CreateCardCommand.class))).willReturn(created);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Comprar pão\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cards/" + created.id()))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.stage").value("BACKLOG"));
    }

    @Test
    void blankTitleReturns400AsProblemJsonWithFieldError() throws Exception {
        mockMvc.perform(post("/api/cards").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void creationAtTheCapReturns409() throws Exception {
        BDDMockito.given(createCardUseCase.create(any(CreateCardCommand.class))).willThrow(new BoardFullException(500));

        mockMvc.perform(post("/api/cards").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Tarefa\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void getBoardAlwaysReturnsAllFourColumns() throws Exception {
        List<BoardSnapshot.Column> columns = Arrays.stream(BoardStage.values())
                .map(stage -> new BoardSnapshot.Column(stage, List.of()))
                .toList();
        BDDMockito.given(viewBoardQuery.load()).willReturn(new BoardSnapshot(columns, 0, 500));

        mockMvc.perform(get("/api/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.length()").value(4))
                .andExpect(jsonPath("$.columns[0].stage").value("BACKLOG"))
                .andExpect(jsonPath("$.maxCards").value(500));
    }

    @Test
    void listCardsFiltersByStage() throws Exception {
        TaskCard inProgress =
                TaskCard.create(CardTitle.of("Em andamento"), CardNote.empty(), BoardStage.IN_PROGRESS, Instant.now());
        List<BoardSnapshot.Column> columns = List.of(
                new BoardSnapshot.Column(BoardStage.BACKLOG, List.of()),
                new BoardSnapshot.Column(BoardStage.IN_PROGRESS, List.of(inProgress)),
                new BoardSnapshot.Column(BoardStage.PAUSED, List.of()),
                new BoardSnapshot.Column(BoardStage.DONE, List.of()));
        BDDMockito.given(viewBoardQuery.load()).willReturn(new BoardSnapshot(columns, 1, 500));

        mockMvc.perform(get("/api/cards").param("stage", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stage").value("IN_PROGRESS"));
    }

    @Test
    void getUnknownCardReturns404() throws Exception {
        BDDMockito.given(viewCardQuery.load(org.mockito.ArgumentMatchers.any(CardId.class)))
                .willThrow(new CardNotFoundException("missing"));

        mockMvc.perform(get("/api/cards/" + CardId.newId()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void invalidStageFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/cards").param("stage", "NOT_A_STAGE")).andExpect(status().isBadRequest());
    }
}
