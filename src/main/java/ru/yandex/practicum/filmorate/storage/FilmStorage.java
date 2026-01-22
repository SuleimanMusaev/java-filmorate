package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {
    Film getFilmById(Long id);

    Collection<Film> getAllFilms();

    Film createFilm(Film film);

    Film updateFilm(Film film);

    Film userLikesFilm(Long id, Long userId);

    Film deleteLikesFilm(Long id, Long userId);

    void deleteFilm(Long filmId);


    //Методы для работы с режисерами
    void saveFilmDirectors(Long filmId, List<Director> directors);

    List<Director> loadDirectors(Film film);

    void deleteFilmDirectors(Long filmId);

    void loadDirectorsForFilms(List<Film> films);

    Collection<Film> getCommonFilms(Long userId, Long friendId);

    Collection<Film> getRecommendations(Long userId);

    List<Film> findFilmsByDirectorId(Long directorId, String sortBy);

    Collection<Film> searchFilms(String query, String by);

    Collection<Film> getPopularFilms(Integer count, Long genreId, Integer year);
}
