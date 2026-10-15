package com.todoproject.adapter.in.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.todoproject.adapter.in.rest.dto.CardResponse;
import com.todoproject.adapter.in.rest.dto.CreateCardRequest;
import com.todoproject.adapter.in.rest.dto.MoveCardRequest;
import com.todoproject.adapter.in.rest.dto.UpdateCardRequest;
import com.todoproject.application.port.in.CreateCardUseCase;
import com.todoproject.application.port.in.CreateCardUseCase.CreateCardCommand;
import com.todoproject.application.port.in.DeleteCardUseCase;
import com.todoproject.application.port.in.DeleteCardUseCase.DeleteCardCommand;
import com.todoproject.application.port.in.EditCardUseCase;
import com.todoproject.application.port.in.EditCardUseCase.EditCardCommand;
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
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Write and single-card read endpoints (contracts/rest-api.yaml). No CSRF token required here --
 * this surface is stateless and cookie-less (research R6, SecurityConfig). */
@RestController
@RequestMapping("/api/cards")
class CardRestController {

    private final ViewCardQuery viewCardQuery;
    private final CreateCardUseCase createCardUseCase;
    private final EditCardUseCase editCardUseCase;
    private final MoveCardUseCase moveCardUseCase;
    private final DeleteCardUseCase deleteCardUseCase;

    CardRestController(
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

    @PostMapping
    ResponseEntity<CardResponse> create(@Valid @RequestBody CreateCardRequest request, HttpServletRequest httpRequest) {
        BoardStage stage = null;
        if (request.stage() != null && !request.stage().isBlank()) {
            stage = BoardStage.parse(request.stage())
                    .orElseThrow(() -> new InvalidCardContentException("stage", ViolationCode.INVALID));
        }
        TaskCard card = createCardUseCase.create(
                new CreateCardCommand(request.title(), request.note(), stage, clientKey(httpRequest)));
        return withETag(ResponseEntity.created(location(card.id())), card);
    }

    @GetMapping("/{cardId}")
    ResponseEntity<CardResponse> get(@PathVariable("cardId") String cardId) {
        TaskCard card = viewCardQuery.load(parseOrNotFound(cardId));
        return withETag(ResponseEntity.ok(), card);
    }

    @PatchMapping("/{cardId}")
    ResponseEntity<CardResponse> update(
            @PathVariable("cardId") String cardId,
            @RequestBody JsonNode body,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest httpRequest) {
        long expectedRevision = requireIfMatch(ifMatch);
        UpdateCardRequest request = UpdateCardRequest.parse(body);
        TaskCard card = editCardUseCase.edit(new EditCardCommand(
                parseOrNotFound(cardId),
                request.newTitle(),
                request.noteChange(),
                expectedRevision,
                clientKey(httpRequest)));
        return withETag(ResponseEntity.ok(), card);
    }

    @PutMapping("/{cardId}/stage")
    ResponseEntity<CardResponse> move(
            @PathVariable("cardId") String cardId,
            @Valid @RequestBody MoveCardRequest request,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest httpRequest) {
        long expectedRevision = requireIfMatch(ifMatch);
        BoardStage target = BoardStage.parse(request.stage())
                .orElseThrow(() -> new InvalidCardContentException("stage", ViolationCode.INVALID));
        TaskCard card = moveCardUseCase.move(
                new MoveCardCommand(parseOrNotFound(cardId), target, expectedRevision, clientKey(httpRequest)));
        return withETag(ResponseEntity.ok(), card);
    }

    @DeleteMapping("/{cardId}")
    ResponseEntity<Void> delete(
            @PathVariable("cardId") String cardId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            HttpServletRequest httpRequest) {
        long expectedRevision = requireIfMatch(ifMatch);
        deleteCardUseCase.delete(
                new DeleteCardCommand(parseOrNotFound(cardId), expectedRevision, clientKey(httpRequest)));
        return ResponseEntity.noContent().build();
    }

    // ---- helpers --------------------------------------------------------

    private CardId parseOrNotFound(String rawId) {
        return CardId.parse(rawId).orElseThrow(() -> new CardNotFoundException(rawId));
    }

    private static String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private static URI location(CardId id) {
        return URI.create("/api/cards/" + id);
    }

    private static ResponseEntity<CardResponse> withETag(ResponseEntity.BodyBuilder builder, TaskCard card) {
        return builder.eTag(Long.toString(card.revision())).body(CardResponse.of(card));
    }

    /**
     * {@code If-Match} is required on every write to an existing card (contracts/rest-api.yaml):
     * its absence is a 428, and the stored revision underneath it is compared by the use case,
     * yielding a 412 on mismatch (research R14).
     */
    private static long requireIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new IfMatchRequiredException();
        }
        String trimmed = ifMatch.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException malformed) {
            throw new IfMatchRequiredException();
        }
    }
}
