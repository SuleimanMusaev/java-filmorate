package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Rating;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class FilmDto {
    Long id;
    String name;
    String description;
    LocalDate releaseDate;
    int duration;
    Set<Genre> genres = new HashSet<>();
    Rating mpa;
    @JsonProperty("directors")
    private List<Director> directors;
}
