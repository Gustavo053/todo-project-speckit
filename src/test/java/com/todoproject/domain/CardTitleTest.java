package com.todoproject.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.ViolationCode;
import com.todoproject.domain.model.CardTitle;
import org.junit.jupiter.api.Test;

/** Covers FR-007, FR-008 and the allowlist policy in research R5. */
class CardTitleTest {

    @Test
    void rejectsNullAsRequired() {
        assertThatThrownBy(() -> CardTitle.of(null))
                .isInstanceOf(InvalidCardContentException.class)
                .satisfies(ex -> assertThat(((InvalidCardContentException) ex).code()).isEqualTo(ViolationCode.REQUIRED));
    }

    @Test
    void rejectsEmptyAsRequired() {
        assertThatThrownBy(() -> CardTitle.of(""))
                .isInstanceOf(InvalidCardContentException.class)
                .satisfies(ex -> assertThat(((InvalidCardContentException) ex).code()).isEqualTo(ViolationCode.REQUIRED));
    }

    @Test
    void rejectsWhitespaceOnlyAsRequired() {
        assertThatThrownBy(() -> CardTitle.of("    "))
                .isInstanceOf(InvalidCardContentException.class)
                .satisfies(ex -> assertThat(((InvalidCardContentException) ex).code()).isEqualTo(ViolationCode.REQUIRED));
    }

    @Test
    void accepts120Characters() {
        String title = "a".repeat(120);
        assertThat(CardTitle.of(title).value()).hasSize(120);
    }

    @Test
    void rejects121CharactersAsTooLong() {
        String title = "a".repeat(121);
        assertThatThrownBy(() -> CardTitle.of(title))
                .isInstanceOf(InvalidCardContentException.class)
                .satisfies(ex -> {
                    InvalidCardContentException invalid = (InvalidCardContentException) ex;
                    assertThat(invalid.code()).isEqualTo(ViolationCode.TOO_LONG);
                    assertThat(invalid.actualLength()).isEqualTo(121);
                });
    }

    @Test
    void doesNotTruncateSilently() {
        String title = "a".repeat(200);
        assertThatThrownBy(() -> CardTitle.of(title)).isInstanceOf(InvalidCardContentException.class);
        // No value is returned on rejection, so there is nothing to have truncated.
    }

    @Test
    void acceptsAccentedPortugueseLetters() {
        assertThat(CardTitle.of("Pagar conta de luz amanhã").value()).isEqualTo("Pagar conta de luz amanhã");
    }

    @Test
    void collapsesInternalWhitespaceRuns() {
        assertThat(CardTitle.of("Comprar   pão   e leite").value()).isEqualTo("Comprar pão e leite");
    }

    @Test
    void rejectsZeroWidthSpace() {
        assertThatThrownBy(() -> CardTitle.of("Tarefa​escondida"))
                .isInstanceOf(InvalidCardContentException.class)
                .satisfies(ex -> assertThat(((InvalidCardContentException) ex).code())
                        .isEqualTo(ViolationCode.FORBIDDEN_CHARACTER));
    }

    @Test
    void rejectsBidirectionalOverride() {
        assertThatThrownBy(() -> CardTitle.of("Tarefa‮override"))
                .isInstanceOf(InvalidCardContentException.class)
                .satisfies(ex -> assertThat(((InvalidCardContentException) ex).code())
                        .isEqualTo(ViolationCode.FORBIDDEN_CHARACTER));
    }

    @Test
    void rejectsControlCharacter() {
        assertThatThrownBy(() -> CardTitle.of("Tarefasino"))
                .isInstanceOf(InvalidCardContentException.class)
                .satisfies(ex -> assertThat(((InvalidCardContentException) ex).code())
                        .isEqualTo(ViolationCode.FORBIDDEN_CHARACTER));
    }

    @Test
    void toStringNeverLeaksTheTitle() {
        assertThat(CardTitle.of("segredo").toString()).doesNotContain("segredo");
    }
}
