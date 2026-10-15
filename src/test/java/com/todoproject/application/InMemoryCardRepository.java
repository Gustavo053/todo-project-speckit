package com.todoproject.application;

import com.todoproject.application.port.out.CardRepository;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A simple in-memory test double for {@link CardRepository}.
 *
 * <p>Mimics the one behaviour that matters for the use-case tests: a save or delete whose
 * {@code revision} does not match what is stored is refused (research R14) -- the same contract
 * {@code CardPersistenceAdapter} implements over JPA.
 */
public class InMemoryCardRepository implements CardRepository {

    private final Map<CardId, TaskCard> storage = new LinkedHashMap<>();
    private final Map<CardId, Long> revisions = new LinkedHashMap<>();

    @Override
    public Optional<TaskCard> findById(CardId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<TaskCard> findAllMostRecentlyChangedFirst() {
        return storage.values().stream()
                .sorted(Comparator.comparing(TaskCard::updatedAt).reversed())
                .toList();
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public TaskCard save(TaskCard card) {
        Long storedRevision = revisions.get(card.id());
        if (storedRevision != null && !storedRevision.equals(card.revision())) {
            throw new StaleCardException(card.id().toString());
        }
        long nextRevision = storedRevision == null ? 0L : storedRevision + 1;
        TaskCard persisted = TaskCard.reconstitute(
                card.id(), card.title(), card.note(), card.stage(), card.createdAt(), card.updatedAt(), nextRevision);
        storage.put(card.id(), persisted);
        revisions.put(card.id(), nextRevision);
        return persisted;
    }

    @Override
    public void delete(TaskCard card) {
        Long storedRevision = revisions.get(card.id());
        if (storedRevision == null) {
            throw new CardNotFoundException(card.id().toString());
        }
        if (!storedRevision.equals(card.revision())) {
            throw new StaleCardException(card.id().toString());
        }
        storage.remove(card.id());
        revisions.remove(card.id());
    }

    /** Puts a card in directly at revision 0, as if it had just been created. */
    public TaskCard seed(TaskCard card) {
        storage.put(card.id(), card);
        revisions.put(card.id(), 0L);
        return card;
    }
}
