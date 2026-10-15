package com.todoproject.adapter.in.web;

import com.todoproject.adapter.in.web.view.BoardView;
import com.todoproject.application.port.in.ViewBoardQuery;
import com.todoproject.domain.error.BoardFullException;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.RateLimitExceededException;
import com.todoproject.domain.error.StaleCardException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Maps domain outcomes to the board with a non-field notice, distinct from the field-level messages
 * a form shows (ux CHK004). No outcome here exposes a stack trace, SQL, or an internal identifier
 * (FR-024, security CHK027, CHK028) -- each branch renders one fixed, pre-written sentence.
 */
@ControllerAdvice(basePackages = "com.todoproject.adapter.in.web")
class WebExceptionHandler {

    private final ViewBoardQuery viewBoardQuery;

    WebExceptionHandler(ViewBoardQuery viewBoardQuery) {
        this.viewBoardQuery = viewBoardQuery;
    }

    @ExceptionHandler(CardNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound(Model model) {
        return boardWithNotice(model, "notice.card.notFound");
    }

    @ExceptionHandler(StaleCardException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String stale(Model model) {
        return boardWithNotice(model, "notice.card.stale");
    }

    @ExceptionHandler(BoardFullException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String boardFull(Model model) {
        return boardWithNotice(model, "notice.board.full");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    String rateLimited(Model model) {
        return boardWithNotice(model, "notice.rate.limited");
    }

    private String boardWithNotice(Model model, String noticeKey) {
        model.addAttribute("board", BoardView.of(viewBoardQuery.load()));
        model.addAttribute("noticeKey", noticeKey);
        return "board";
    }
}
