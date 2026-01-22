package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.GenreDbStorage;
import ru.yandex.practicum.filmorate.dao.RatingDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final RatingDbStorage ratingDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final DirectorStorage directorStorage;
    private final EventStorage eventStorage;

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("ratingDbStorage") RatingDbStorage ratingDbStorage,
                       @Qualifier("genreDbStorage") GenreDbStorage genreDbStorage,
                       DirectorStorage directorStorage,
                       EventStorage eventStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.ratingDbStorage = ratingDbStorage;
        this.genreDbStorage = genreDbStorage;
        this.directorStorage = directorStorage;
        this.eventStorage = eventStorage;
    }

    public Film getFilmById(Long id) {
        Film film = filmStorage.getFilmById(id);
        if (film == null) {
            throw new NotFoundException("Такого фильма нет в списке!");
        }
        return film;
    }

    public Film userLikesFilm(Long id, Long userId) {
        Film film = getFilmById(id);//Проверка на существование фильма
        userStorage.getUserById(userId);//Проверка на существование юзера

        Film result = filmStorage.userLikesFilm(id, userId);

        eventStorage.addEvent(userId, id, "LIKE", "ADD");

        return result;
    }

    public Film deleteLikesFilm(Long id, Long userId) {
        Film result = filmStorage.deleteLikesFilm(id, userId);

        eventStorage.addEvent(userId, id, "LIKE", "REMOVE");

        return result;
    }

    public Collection<Film> getCommonFilms(Long userId, Long friendId) {
        userStorage.getUserById(userId);
        userStorage.getUserById(friendId);
        return filmStorage.getCommonFilms(userId, friendId);
    }

    public Collection<Film> getPopularFilms(Integer count, Long genreId, Integer year) {
        return filmStorage.getPopularFilms(count, genreId, year);
    }

    public List<Film> sortingToDown() {
        ArrayList<Film> listFilms = new ArrayList<>(filmStorage.getAllFilms());
        listFilms.sort((Film film1, Film film2) ->
                Integer.compare(film2.getLikes().size(), film1.getLikes().size())
        );
        return listFilms;
    }

    public Collection<Film> searchFilms(String query, String by) {
        if (query == null || by == null) {
            throw new ValidationException("Параметры query и by не могут быть null");
        }

        List<String> validParams = List.of("director", "title", "director,title", "title,director");
        if (!validParams.contains(by)) {
            throw new ValidationException("Параметр 'by' указан некорректно");
        }

        return filmStorage.searchFilms(query, by);
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public Film createFilm(Film film) {
        validateFilm(film);
        ratingDbStorage.getRatingById(film.getMpa().getId());
        if (film.getGenres() != null) {
            for (Genre g : film.getGenres()) {
                genreDbStorage.getGenreById(g.getId());
            }
        }
        if (film.getDirectors() != null) {
            for (Director d : film.getDirectors()) {
                directorStorage.findById(d.getId());
            }
        }
        return filmStorage.createFilm(film);
    }

    public Film updateFilm(Film film) {
        validateFilm(film);
        return filmStorage.updateFilm(film);
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Name is empty");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Description too long");
        }

        if (film.getDuration() <= 0) {
            throw new ValidationException("Duration must be positive");
        }

        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("MPA is missing");
        }
        if (film.getDirectors() != null) {
            for (Director d : film.getDirectors()) {
                directorStorage.findById(d.getId());
            }
        }
    }

    public void deleteFilm(Long filmId) {
        getFilmById(filmId);
        filmStorage.deleteFilm(filmId);
    }

    public List<Film> findFilmsByDirectorId(Long directorId, String sortBy) {
        // Проверяем существование режиссера
        directorStorage.findById(directorId);

        if (!"year".equals(sortBy) && !"likes".equals(sortBy)) {
            throw new ValidationException("Параметр sortBy должен быть 'year' или 'likes'");
        }

        return filmStorage.findFilmsByDirectorId(directorId, sortBy);
    }
}
