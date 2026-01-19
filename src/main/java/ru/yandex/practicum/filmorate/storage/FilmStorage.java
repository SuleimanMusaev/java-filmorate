package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;

public interface FilmStorage {
    Film getFilmById(Long id);

    Collection<Film> getAllFilms();

    Film createFilm(Film film);

    Film updateFilm(Film film);

    Film userLikesFilm(Long id, Long userId);

    Film deleteLikesFilm(Long id, Long userId);

    void deleteFilm(Long filmId);

    Collection<Film> getCommonFilms(Long userId, Long friendId);

    Collection<Film> getRecommendations(Long userId);
}
