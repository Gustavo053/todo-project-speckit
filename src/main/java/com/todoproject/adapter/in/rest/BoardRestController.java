package com.todoproject.adapter.in.rest;

import com.todoproject.adapter.in.rest.dto.BoardResponse;
import com.todoproject.adapter.in.rest.dto.CardResponse;
import com.todoproject.application.port.in.ViewBoardQuery;
import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.ViolationCode;
import com.todoproject.domain.model.BoardStage;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only endpoints over the board (contracts/rest-api.yaml). */
@RestController
@RequestMapping("/api")
class BoardRestController {

    private final ViewBoardQuery viewBoardQuery;

    BoardRestController(ViewBoardQuery viewBoardQuery) {
        this.viewBoardQuery = viewBoardQuery;
    }

    @GetMapping("/board")
    ResponseEntity<BoardResponse> getBoard() {
        return ResponseEntity.ok(BoardResponse.of(viewBoardQuery.load()));
    }

    @GetMapping("/cards")
    ResponseEntity<List<CardResponse>> listCards(@RequestParam(name = "stage", required = false) String stage) {
        BoardStage filter;
        if (stage != null) {
            filter = BoardStage.parse(stage)
                    .orElseThrow(() -> new InvalidCardContentException("stage", ViolationCode.INVALID));
        } else {
            filter = null;
        }
        List<CardResponse> cards = viewBoardQuery.load().columns().stream()
                .filter(column -> filter == null || column.stage() == filter)
                .flatMap(column -> column.cards().stream())
                .map(CardResponse::of)
                .toList();
        return ResponseEntity.ok(cards);
    }
}
