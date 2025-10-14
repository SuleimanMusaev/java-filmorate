package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Film.
 */
@Data
public class Film {
    public static final Instant MIN_DATE = LocalDate.of(1895, 12, 28)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant();
    Long id;
    String name;
    String description;
    Instant releaseDate;
    Duration duration;
}
