package com.todoproject.adapter.in.web.view;

import com.todoproject.domain.model.BoardStage;
import com.todoproject.domain.model.TaskCard;
import java.util.Arrays;
import java.util.List;

/**
 * One card, shaped for rendering.
 *
 * <p>Holds no domain types beyond the stage enum, so the template cannot reach into the aggregate.
 *
 * @param noteHasMore whether to show the "there is more text" affordance (FR-005a). The two-line
 *     clamp itself is CSS; this flag is the server's best available signal, since no server-side
 *     measurement can know where the browser will break a line. It is an explicit approximation,
 *     not a guess at layout.
 */
public record CardView(
        String id,
        String title,
        String note,
        boolean hasNote,
        boolean noteHasMore,
        BoardStage stage,
        long revision,
        List<BoardStage> moveTargets) {

    /** Beyond this many characters a note certainly exceeds two lines on a narrow screen. */
    private static final int PREVIEW_THRESHOLD = 90;

    public static CardView of(TaskCard card) {
        String note = card.note().orNull();
        boolean hasNote = note != null;
        boolean hasMore = hasNote && (note.contains("\n") || note.codePointCount(0, note.length()) > PREVIEW_THRESHOLD);
        return new CardView(
                card.id().toString(),
                card.title().value(),
                note,
                hasNote,
                hasMore,
                card.stage(),
                card.revision(),
                moveTargetsFor(card.stage()));
    }

    /** Every stage except the card's own — one submit button each (research R7). */
    private static List<BoardStage> moveTargetsFor(BoardStage current) {
        return Arrays.stream(BoardStage.values())
                .filter(stage -> stage != current)
                .toList();
    }
}
