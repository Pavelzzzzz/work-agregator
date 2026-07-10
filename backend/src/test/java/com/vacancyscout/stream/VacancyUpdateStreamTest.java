package com.vacancyscout.stream;

import com.vacancyscout.dto.EventType;
import com.vacancyscout.dto.VacancyUpdateEvent;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class VacancyUpdateStreamTest {
  @Test
  void should_emit_events_in_order() {
    VacancyUpdateStream stream = new VacancyUpdateStream();

    VacancyUpdateEvent e1 =
        new VacancyUpdateEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Title1",
            "CompanyA",
            LocalDateTime.now(),
            "HH_RU",
            EventType.NEW,
            "hh.ru");
    VacancyUpdateEvent e2 =
        new VacancyUpdateEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Title2",
            "CompanyB",
            LocalDateTime.now(),
            "HH_RU",
            EventType.NEW,
            "hh.ru");

    Flux<VacancyUpdateEvent> flux = stream.stream().take(2);

    StepVerifier.create(flux)
        .then(
            () -> {
              stream.publish(e1);
              stream.publish(e2);
            })
        .expectNext(e1)
        .expectNext(e2)
        .expectComplete()
        .verify();
  }
}
