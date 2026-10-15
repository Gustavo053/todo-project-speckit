package com.todoproject.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todoproject.adapter.in.web.CardController;
import com.todoproject.application.port.in.CreateCardUseCase;
import com.todoproject.application.port.in.CreateCardUseCase.CreateCardCommand;
import com.todoproject.application.port.in.DeleteCardUseCase;
import com.todoproject.application.port.in.EditCardUseCase;
import com.todoproject.application.port.in.MoveCardUseCase;
import com.todoproject.application.port.in.ViewCardQuery;
import com.todoproject.config.SecurityConfig;
import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.ViolationCode;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardNote;
import com.todoproject.domain.model.CardTitle;
import com.todoproject.domain.model.TaskCard;
import java.time.Instant;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers FR-006, FR-007, FR-008, SC-002: a valid create redirects to the new card; a rejected one
 * re-renders the form at 422 with the typed input preserved.
 */
@WebMvcTest(CardController.class)
@Import(SecurityConfig.class)
class CreateCardFlowTest {

    @Autowired
    private MockMvc mockMvc;

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
    void validSubmissionRedirectsToTheNewCard() throws Exception {
        TaskCard created =
                TaskCard.create(CardTitle.of("Pagar conta de luz"), CardNote.empty(), BoardStage.BACKLOG, Instant.now());
        BDDMockito.given(createCardUseCase.create(any(CreateCardCommand.class))).willReturn(created);

        mockMvc.perform(post("/cards").param("title", "Pagar conta de luz").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/#card-*"));
    }

    @Test
    void blankTitleIsRejectedWithFieldMessageAndPreservedNote() throws Exception {
        MvcResult result = mockMvc.perform(post("/cards")
                        .param("title", "")
                        .param("note", "um texto qualquer")
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        var document = Jsoup.parse(result.getResponse().getContentAsString());
        assertThat(document.select(".field__error")).isNotEmpty();
        assertThat(document.select("#note").attr("value") + document.select("#note").text())
                .contains("um texto qualquer");
    }

    @Test
    void whitespaceOnlyTitleIsRejected() throws Exception {
        mockMvc.perform(post("/cards").param("title", "   ").with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void titleOver120CharactersIsRejectedWithoutTruncation() throws Exception {
        String tooLong = "a".repeat(121);

        mockMvc.perform(post("/cards").param("title", tooLong).with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void titleWithAZeroWidthSpaceIsRejectedByTheDomainLayer() throws Exception {
        BDDMockito.given(createCardUseCase.create(any(CreateCardCommand.class)))
                .willThrow(new InvalidCardContentException("title", ViolationCode.FORBIDDEN_CHARACTER));

        mockMvc.perform(post("/cards").param("title", "Tarefa​escondida").with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }
}
