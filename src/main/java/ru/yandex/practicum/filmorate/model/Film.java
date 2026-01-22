package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Film {
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
    private List<Director> directors = new ArrayList<>();
}
