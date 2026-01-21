package ru.yandex.practicum.filmorate.model;

import lombok.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Film {
    public static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    private Long id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private int duration;

    @Builder.Default
    private Set<Long> likes = new HashSet<>();

    @Builder.Default
    private Set<Genre> genres = new HashSet<>();

    private Rating mpa;

    @Builder.Default
    private Set<Director> directors = new HashSet<>();
}
