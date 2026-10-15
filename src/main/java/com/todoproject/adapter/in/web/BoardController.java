package com.todoproject.adapter.in.web;

import com.todoproject.adapter.in.web.view.BoardView;
import com.todoproject.application.port.in.ViewBoardQuery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the board itself (FR-001, FR-003, FR-004). */
@Controller
class BoardController {

    private final ViewBoardQuery viewBoardQuery;

    BoardController(ViewBoardQuery viewBoardQuery) {
        this.viewBoardQuery = viewBoardQuery;
    }

    @GetMapping("/")
    String board(Model model) {
        model.addAttribute("board", BoardView.of(viewBoardQuery.load()));
        return "board";
    }
}
