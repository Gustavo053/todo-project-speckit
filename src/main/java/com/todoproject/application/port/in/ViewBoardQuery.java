package com.todoproject.application.port.in;

/** Inbound port: read the board. */
public interface ViewBoardQuery {

    BoardSnapshot load();
}
