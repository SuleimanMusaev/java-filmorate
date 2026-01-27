package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Collection<Film> getAllFilms();

    Film createFilm(Film film);

    Film updateFilm(Film film);

    Film getFilmById(Long id);

    Optional<Film> findById(Long id);

    void deleteFilm(Long filmId);

    Film userLikesFilm(Long id, Long userId);

    Film deleteLikesFilm(Long id, Long userId);

    Collection<Film> getPopularFilms(Integer count, Long genreId, Integer year);

    Collection<Film> getCommonFilms(Long userId, Long friendId);

    Collection<Film> searchFilms(String query, String by);

    Collection<Film> getRecommendations(Long userId);

    List<Film> findFilmsByDirectorId(Long directorId, String sortBy);

    void saveFilmDirectors(Long filmId, List<Director> directors);

    void deleteFilmDirectors(Long filmId);

    void loadDirectorsForFilms(List<Film> films);

    List<Director> loadDirectors(Film film);
}
