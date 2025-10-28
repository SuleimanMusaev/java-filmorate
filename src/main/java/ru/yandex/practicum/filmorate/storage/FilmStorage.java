package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {
    Film create(Film film);

    Film update(Film newFilm);

    void delete(Film film);

    Collection<Film> getFilms();

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    int getLikesCount(Long filmId);

    List<Film> getTopFilms(int count);
}
