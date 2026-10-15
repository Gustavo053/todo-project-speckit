package com.todoproject.config;

import com.todoproject.application.port.in.CreateCardUseCase;
import com.todoproject.application.port.in.DeleteCardUseCase;
import com.todoproject.application.port.in.EditCardUseCase;
import com.todoproject.application.port.in.MoveCardUseCase;
import com.todoproject.application.port.in.ViewBoardQuery;
import com.todoproject.application.port.in.ViewCardQuery;
import com.todoproject.application.port.out.CardRepository;
import com.todoproject.application.port.out.ClockPort;
import com.todoproject.application.port.out.WriteRateLimiterPort;
import com.todoproject.application.service.CreateCardService;
import com.todoproject.application.service.DeleteCardService;
import com.todoproject.application.service.EditCardService;
import com.todoproject.application.service.MoveCardService;
import com.todoproject.application.service.ViewBoardService;
import com.todoproject.application.service.ViewCardService;
import com.todoproject.application.service.WriteGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for the application core, and the reason the core itself carries no Spring
 * annotations.
 *
 * <p>Marking the services {@code @Service} would be the shorter route, but
 * {@code org.springframework.stereotype.Service} is a framework type and constitution principle II
 * forbids those in {@code application..} — the ArchUnit suite fails the build on it. Declaring the
 * beans here keeps the services plain constructor-injected classes that unit-test without a
 * container, and confines the framework to {@code config}.
 */
@Configuration
class UseCaseConfiguration {

    @Bean
    WriteGuard writeGuard(WriteRateLimiterPort rateLimiter) {
        return new WriteGuard(rateLimiter);
    }

    @Bean
    ViewBoardQuery viewBoardQuery(CardRepository cardRepository, BoardProperties properties) {
        return new ViewBoardService(cardRepository, properties.maxCards());
    }

    @Bean
    ViewCardQuery viewCardQuery(CardRepository cardRepository) {
        return new ViewCardService(cardRepository);
    }

    @Bean
    CreateCardUseCase createCardUseCase(
            CardRepository cardRepository, ClockPort clock, WriteGuard writeGuard, BoardProperties properties) {
        return new CreateCardService(cardRepository, clock, writeGuard, properties.maxCards());
    }

    @Bean
    EditCardUseCase editCardUseCase(CardRepository cardRepository, ClockPort clock, WriteGuard writeGuard) {
        return new EditCardService(cardRepository, clock, writeGuard);
    }

    @Bean
    MoveCardUseCase moveCardUseCase(CardRepository cardRepository, ClockPort clock, WriteGuard writeGuard) {
        return new MoveCardService(cardRepository, clock, writeGuard);
    }

    @Bean
    DeleteCardUseCase deleteCardUseCase(CardRepository cardRepository, WriteGuard writeGuard) {
        return new DeleteCardService(cardRepository, writeGuard);
    }
}
