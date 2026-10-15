package com.todoproject.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to the card table. Package-private: only the adapter beside it may use it. */
public interface CardJpaRepository extends JpaRepository<CardJpaEntity, UUID> {

    List<CardJpaEntity> findAllByOrderByUpdatedAtDesc();
}
