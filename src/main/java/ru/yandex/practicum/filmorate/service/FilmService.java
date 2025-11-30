package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.GenreDbStorage;
import ru.yandex.practicum.filmorate.dao.RatingDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
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

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("ratingDbStorage") RatingDbStorage ratingDbStorage,
                       @Qualifier("genreDbStorage")GenreDbStorage genreDbStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.ratingDbStorage = ratingDbStorage;
        this.genreDbStorage = genreDbStorage;
    }

    public Film getFilmById(Long id) {
        try {
            return filmStorage.getFilmById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Такого фильма нет в списке!");
        }
    }

    public Film userLikesFilm(Long id, Long userId) {
        return filmStorage.userLikesFilm(id, userId);
    }

    public Film deleteLikesFilm(Long id, Long userId) {
        return filmStorage.deleteLikesFilm(id, userId);
    }

    public Collection<Film> listFirstCountFilm(int count) {
        Collection<Film> films;
        films = sortingToDown().stream()
                .limit(count)
                .toList();
        return films;
    }

    public List<Film> sortingToDown() {
        ArrayList<Film> listFilms = new ArrayList<>(filmStorage.getAllFilms());
        listFilms.sort((Film film1, Film film2) ->
                Integer.compare(film2.getLikes().size(), film1.getLikes().size())
        );
        return listFilms;
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

        if (film.getMpa() == null) {
            throw new ValidationException("MPA is missing");
        }
    }
}
