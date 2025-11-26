package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;

@Component("inMemoryFilmStorage")
@RequiredArgsConstructor
public class InMemoryFilmStorage implements FilmStorage {
    private static final Logger log = LoggerFactory.getLogger(InMemoryFilmStorage.class);
    UserStorage userStorage;

    private final Map<Long, Film> films = new HashMap<>();

    @Override
    public Film getFilmById(Long id) {
        return films.get(id);
    }

    @Override
    public Collection<Film> getAllFilms() {
        return films.values();
    }

    @Override
    public Film createFilm(Film film) {
        if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года!");
        }
        film.setId(getNextId());
        log.debug("Валидация пройдена.");
        films.put(film.getId(), film);
        log.debug("Фильм добавлен в список.");
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        // validateFilm(film);
        if (film.getId() == null) {
            throw new ValidationException("Id должен быть указан!");
        }
        if (films.containsKey(film.getId())) {
            if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
                throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года!");
            }
            films.put(film.getId(), film);
            return film;
        } else throw new NotFoundException("Такого фильма нет в списке!");
    }

    public Film userLikesFilm(Long id, Long userId) {
        Film film = getFilmById(id);
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        if (film == null) {
            throw new NotFoundException("Такого фильма нет в списке!");
        }
        film.getLikes().add(userId);
        return film;
    }

    public Film deleteLikesFilm(Long id, Long userId) {
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("Такого юзера нет!");
        }
        Film film = getFilmById(id);
        if (film == null) {
            throw new NotFoundException("Такого фильма нет в списке!");
        }
        film.getLikes().remove(userId);
        return film;
    }

    private Long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
