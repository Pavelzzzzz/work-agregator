package com.vacancyscout.stream;

import com.vacancyscout.dto.VacancyUpdateEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class VacancyUpdateStream {
    private final Sinks.Many<VacancyUpdateEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(VacancyUpdateEvent event) {
        sink.tryEmitNext(event);
    }

    public Flux<VacancyUpdateEvent> stream() {
        return sink.asFlux();
    }
}
