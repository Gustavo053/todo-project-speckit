package com.todoproject.adapter.out.persistence;

import com.todoproject.application.port.out.CardRepository;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.StaleCardException;
import com.todoproject.domain.model.CardId;
import com.todoproject.domain.model.TaskCard;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the card storage port over JPA.
 *
 * <p>Transactions live here rather than in the application layer, because {@code @Transactional} is
 * a Spring type and the core must not carry framework annotations (constitution principle II). That
 * is sound rather than a workaround: a use case is load-mutate-save, and the conditional revision
 * check below makes the save safe without an enclosing transaction — a concurrent change is
 * refused instead of overwritten (research R14).
 */
@Repository
public class CardPersistenceAdapter implements CardRepository {

    private final CardJpaRepository jpaRepository;

    CardPersistenceAdapter(CardJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaskCard> findById(CardId id) {
        return jpaRepository.findById(id.value()).map(CardMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskCard> findAllMostRecentlyChangedFirst() {
        return jpaRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(CardMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return jpaRepository.count();
    }

    @Override
    @Transactional
    public TaskCard save(TaskCard card) {
        Optional<CardJpaEntity> existing = jpaRepository.findById(card.id().value());
        if (existing.isEmpty()) {
            return CardMapper.toDomain(jpaRepository.saveAndFlush(CardMapper.toNewEntity(card)));
        }

        CardJpaEntity entity = existing.get();
        requireSameRevision(entity, card);
        entity.apply(card.title().value(), card.note().orNull(), card.stage(), card.updatedAt());
        try {
            return CardMapper.toDomain(jpaRepository.saveAndFlush(entity));
        } catch (OptimisticLockingFailureException concurrentChange) {
            throw new StaleCardException(card.id().toString());
        }
    }

    @Override
    @Transactional
    public void delete(TaskCard card) {
        CardJpaEntity entity = jpaRepository
                .findById(card.id().value())
                .orElseThrow(() -> new CardNotFoundException(card.id().toString()));
        requireSameRevision(entity, card);
        try {
            jpaRepository.delete(entity);
            jpaRepository.flush();
        } catch (OptimisticLockingFailureException concurrentChange) {
            throw new StaleCardException(card.id().toString());
        }
    }

    /**
     * Refuses the write when the stored row has moved on since the caller read it. This is the same
     * rule the use case already applied, re-checked against the row as it is right now.
     */
    private void requireSameRevision(CardJpaEntity entity, TaskCard card) {
        Long stored = entity.getRevision();
        long expected = card.revision();
        if (!Objects.equals(stored == null ? 0L : stored, expected)) {
            throw new StaleCardException(card.id().toString());
        }
    }
}
