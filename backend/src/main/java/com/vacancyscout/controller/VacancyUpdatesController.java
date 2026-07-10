package com.vacancyscout.controller;

import com.vacancyscout.dto.VacancyUpdateEvent;
import com.vacancyscout.stream.VacancyUpdateStream;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/vacancies")
public class VacancyUpdatesController {
    private final VacancyUpdateStream vacancyUpdateStream;

    public VacancyUpdatesController(VacancyUpdateStream vacancyUpdateStream) {
        this.vacancyUpdateStream = vacancyUpdateStream;
    }

    @GetMapping(value = "/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<VacancyUpdateEvent> updates(@RequestParam(value = "since", required = false) String since) {
        LocalDateTime sinceTime;
        if (since != null) {
            try {
                sinceTime = LocalDateTime.parse(since);
            } catch (DateTimeParseException ex) {
                sinceTime = LocalDateTime.MIN;
            }
        } else {
            sinceTime = LocalDateTime.MIN;
        }
        LocalDateTime effectiveSince = sinceTime;
        return vacancyUpdateStream.stream()
                .filter(ev -> ev.postedAt() != null && ev.postedAt().isAfter(effectiveSince));
    }
}
