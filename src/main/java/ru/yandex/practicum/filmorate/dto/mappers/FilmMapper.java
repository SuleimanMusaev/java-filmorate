package ru.yandex.practicum.filmorate.dto.mappers;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.model.Film;

@Component
public class FilmMapper {

    public Film mapToFilm(FilmDto filmDto) {
        return Film.builder()
                .id(filmDto.getId())
                .name(filmDto.getName())
                .description(filmDto.getDescription())
                .releaseDate(filmDto.getReleaseDate())
                .duration(filmDto.getDuration());
        .mpa(filmDto.getMpa())
                .genres(filmDto.getGenres());
        .directors(filmDto.getDirectors())
                .build();

    }

    public static FilmDto mapToFilmDto(Film film) {
        return FilmDto.builder()
                .id(film.getId())
                .name(film.getName())
                .description(filmDto.getDescription())
                .releaseDate(filmDto.getReleaseDate())
                .duration(filmDto.getDuration());
        .mpa(filmDto.getMpa())
                .genres(filmDto.getGenres());
        .directors(filmDto.getDirectors())
                .build();
    }
}
