package com.todoproject.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.ViolationCode;
import com.todoproject.domain.model.CardNote;
import org.junit.jupiter.api.Test;

/** Covers FR-008 and the absent/blank-is-the-same-thing rule in data-model.md. */
class CardNoteTest {

    @Test
    void nullIsAbsent() {
        assertThat(CardNote.of(null).isPresent()).isFalse();
    }

    @Test
    void whitespaceOnlyIsStoredAsAbsent() {
        CardNote note = CardNote.of("   \n   ");
        assertThat(note.isPresent()).isFalse();
        assertThat(note.orNull()).isNull();
    }

    @Test
    void accepts2000Characters() {
        String note = "a".repeat(2000);
        assertThat(CardNote.of(note).orNull()).hasSize(2000);
    }

    @Test
    void rejects2001CharactersAsTooLong() {
        String note = "a".repeat(2001);
        assertThatThrownBy(() -> CardNote.of(note))
                .isInstanceOf(InvalidCardContentException.class)
                .satisfies(ex -> {
                    InvalidCardContentException invalid = (InvalidCardContentException) ex;
                    assertThat(invalid.code()).isEqualTo(ViolationCode.TOO_LONG);
                    assertThat(invalid.actualLength()).isEqualTo(2001);
                });
    }

    @Test
    void normalizesWindowsLineEndings() {
        assertThat(CardNote.of("linha 1\r\nlinha 2").orNull()).isEqualTo("linha 1\nlinha 2");
    }

    @Test
    void normalizesLoneCarriageReturn() {
        assertThat(CardNote.of("linha 1\rlinha 2").orNull()).isEqualTo("linha 1\nlinha 2");
    }

    @Test
    void preservesLineStructure() {
        assertThat(CardNote.of("um\ndois\ntrês").orNull()).isEqualTo("um\ndois\ntrês");
    }

    @Test
    void rejectsZeroWidthSpace() {
        assertThatThrownBy(() -> CardNote.of("nota​escondida"))
                .isInstanceOf(InvalidCardContentException.class)
                .satisfies(ex -> assertThat(((InvalidCardContentException) ex).code())
                        .isEqualTo(ViolationCode.FORBIDDEN_CHARACTER));
    }

    @Test
    void toStringNeverLeaksTheNote() {
        assertThat(CardNote.of("segredo").toString()).doesNotContain("segredo");
    }
}
