package com.todoproject.adapter.in.web;

import com.todoproject.adapter.in.web.dto.CreateCardForm;
import com.todoproject.adapter.in.web.dto.DeleteCardForm;
import com.todoproject.adapter.in.web.dto.EditCardForm;
import com.todoproject.adapter.in.web.dto.MoveCardForm;
import com.todoproject.adapter.in.web.view.CardView;
import com.todoproject.application.port.in.CreateCardUseCase;
import com.todoproject.application.port.in.CreateCardUseCase.CreateCardCommand;
import com.todoproject.application.port.in.DeleteCardUseCase;
import com.todoproject.application.port.in.DeleteCardUseCase.DeleteCardCommand;
import com.todoproject.application.port.in.EditCardUseCase;
import com.todoproject.application.port.in.EditCardUseCase.EditCardCommand;
import com.todoproject.application.port.in.EditCardUseCase.NoteChange;
import com.todoproject.application.port.in.MoveCardUseCase;
import com.todoproject.application.port.in.MoveCardUseCase.MoveCardCommand;
import com.todoproject.application.port.in.ViewCardQuery;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.ViolationCode;
import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Every card action as a plain form submission (contracts/html-routes.md). No scripting is
 * required for any of this (FR-030) -- each write answers 303 See Other on success, or re-renders
 * the originating form with status 422 and the typed input intact on rejection.
 */
@Controller
class CardController {

    private final ViewCardQuery viewCardQuery;
    private final CreateCardUseCase createCardUseCase;
    private final EditCardUseCase editCardUseCase;
    private final MoveCardUseCase moveCardUseCase;
    private final DeleteCardUseCase deleteCardUseCase;

    CardController(
            ViewCardQuery viewCardQuery,
            CreateCardUseCase createCardUseCase,
            EditCardUseCase editCardUseCase,
            MoveCardUseCase moveCardUseCase,
            DeleteCardUseCase deleteCardUseCase) {
        this.viewCardQuery = viewCardQuery;
        this.createCardUseCase = createCardUseCase;
        this.editCardUseCase = editCardUseCase;
        this.moveCardUseCase = moveCardUseCase;
        this.deleteCardUseCase = deleteCardUseCase;
    }

    // ---- Create (US1) ----------------------------------------------------

    @GetMapping("/cards/new")
    String newCardForm(@RequestParam(name = "stage", required = false) String stage, Model model) {
        CreateCardForm form = new CreateCardForm();
        form.setStage(stage == null ? "" : stage);
        model.addAttribute("form", form);
        model.addAttribute("mode", "create");
        return "card-form";
    }

    @PostMapping("/cards")
    String createCard(
            @Valid @ModelAttribute("form") CreateCardForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return rejectedForm(model, response);
        }

        BoardStage stage = BoardStage.parse(form.getStage()).orElse(null);
        try {
            TaskCard card = createCardUseCase.create(new CreateCardCommand(
                    form.getTitle(), blankToNull(form.getNote()), stage, clientKey(request)));
            return "redirect:/#card-" + card.id();
        } catch (InvalidCardContentException invalid) {
            applyDomainViolation(bindingResult, invalid);
            model.addAttribute("mode", "create");
            return rejectedForm(model, response);
        }
    }

    // ---- Edit (US3) --------------------------------------------------------

    @GetMapping("/cards/{id}/edit")
    String editCardForm(@PathVariable("id") String id, Model model) {
        TaskCard card = loadOrNotFound(id);
        EditCardForm form = new EditCardForm();
        form.setTitle(card.title().value());
        form.setNote(card.note().orNull() == null ? "" : card.note().orNull());
        form.setRevision(card.revision());
        model.addAttribute("form", form);
        model.addAttribute("mode", "edit");
        model.addAttribute("cardId", id);
        return "card-form";
    }

    @PostMapping("/cards/{id}")
    String editCard(
            @PathVariable("id") String id,
            @Valid @ModelAttribute("form") EditCardForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("cardId", id);
            return rejectedForm(model, response);
        }

        CardId cardId = parseOrNotFound(id);
        try {
            editCardUseCase.edit(new EditCardCommand(
                    cardId,
                    form.getTitle(),
                    NoteChange.to(blankToNull(form.getNote())),
                    form.getRevision(),
                    clientKey(request)));
            return "redirect:/#card-" + id;
        } catch (InvalidCardContentException invalid) {
            applyDomainViolation(bindingResult, invalid);
            model.addAttribute("mode", "edit");
            model.addAttribute("cardId", id);
            return rejectedForm(model, response);
        }
    }

    // ---- Move (US2) ---------------------------------------------------------

    @PostMapping("/cards/{id}/move")
    String moveCard(
            @PathVariable("id") String id,
            @Valid @ModelAttribute("moveForm") MoveCardForm form,
            BindingResult bindingResult,
            HttpServletRequest request) {
        CardId cardId = parseOrNotFound(id);
        BoardStage target = BoardStage.parse(form.getStage())
                .orElseThrow(() -> new InvalidCardContentException("stage", ViolationCode.INVALID));
        moveCardUseCase.move(new MoveCardCommand(cardId, target, form.getRevision(), clientKey(request)));
        return "redirect:/#card-" + id;
    }

    // ---- Delete (US4) -------------------------------------------------------

    @GetMapping("/cards/{id}/delete")
    String deleteConfirmation(@PathVariable("id") String id, Model model) {
        TaskCard card = loadOrNotFound(id);
        DeleteCardForm form = new DeleteCardForm();
        form.setRevision(card.revision());
        model.addAttribute("form", form);
        model.addAttribute("card", CardView.of(card));
        return "delete-confirm";
    }

    @PostMapping("/cards/{id}/delete")
    String deleteCard(
            @PathVariable("id") String id,
            @Valid @ModelAttribute("form") DeleteCardForm form,
            HttpServletRequest request) {
        CardId cardId = parseOrNotFound(id);
        deleteCardUseCase.delete(new DeleteCardCommand(cardId, form.getRevision(), clientKey(request)));
        return "redirect:/";
    }

    // ---- helpers --------------------------------------------------------

    private TaskCard loadOrNotFound(String rawId) {
        return viewCardQuery.load(parseOrNotFound(rawId));
    }

    private CardId parseOrNotFound(String rawId) {
        return CardId.parse(rawId).orElseThrow(() -> new CardNotFoundException(rawId));
    }

    private static String blankToNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw;
    }

    private static String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * Rejected writes re-render the originating form with 422, not 200 -- nothing was created or
     * changed (contracts/html-routes.md).
     */
    private String rejectedForm(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        return "card-form";
    }

    private static void applyDomainViolation(BindingResult bindingResult, InvalidCardContentException invalid) {
        FieldError existing = bindingResult.getFieldError(invalid.field());
        if (existing != null) {
            return;
        }
        String code = "validation." + invalid.field() + "." + invalid.code();
        Object[] args = invalid.actualLength() >= 0 ? new Object[] {invalid.actualLength()} : new Object[0];
        bindingResult.addError(new FieldError(
                bindingResult.getObjectName(), invalid.field(), null, false, new String[] {code}, args, code));
    }
}
