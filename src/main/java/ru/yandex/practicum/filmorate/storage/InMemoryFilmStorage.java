package ru.yandex.practicum.filmorate.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private static final int MAX_LENGTH_OF_DESCRIPTION = 200;
    private static final LocalDate MIN_DATE = LocalDate.of(1895, 12, 28);

    private static final Logger log = LoggerFactory.getLogger(InMemoryFilmStorage.class);
    private final Map<Long, Film> films = new HashMap<>();
    private final InMemoryUserStorage userStorage;

    @Autowired
    public InMemoryFilmStorage(InMemoryUserStorage userStorage) {
        this.userStorage = userStorage;
    }

    @Override
    public Collection<Film> getFilms() {
        log.info("GET-запрос: получение списка всех фильмов({} шт)", films.size());
        return new ArrayList<>(films.values());
    }

    @Override
    public Film create(Film film) {
        log.info("Создание фильма...: {}", film.getName());
        try {
            validateFilm(film);

            film.setId(getNextId());
            films.put(film.getId(), film);
            log.info("Фильм успешно создан: id={}, name='{}'", film.getId(), film.getName());
            return film;
        } catch (ValidationException e) {
            log.error("Ошибка при создании фильма '{}': {}", film.getName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public void delete(Film film) {
        log.info("Удаление фильма...: {}", film.getName());
        try {
            if (film.getId() == null || !films.containsKey(film.getId())) {
                log.warn("Фильм '{}' не найден", film.getName());
                throw new NotFoundException("Фильм с названием = " + film.getName() + " не найден");
            }

            films.remove(film.getId());
            log.info("Фильм успешно удален: id={}, name='{}'", film.getId(), film.getName());
        } catch (ValidationException | NotFoundException e) {
            log.error("Ошибка при удалении фильма id={}: {}", film.getId(), e.getMessage());
            throw e;
        }
    }

    @Override
    public Film update(Film newFilm) {
        log.info("Попытка обновить фильм: id={}, name='{}'", newFilm.getId(), newFilm.getName());
        try {
            if (newFilm.getId() == null || !films.containsKey(newFilm.getId())) {
                log.warn("Ошибка: фильм с названием={} не найден", newFilm.getName());
                throw new NotFoundException("Фильм с названием = " + newFilm.getName() + " не найден");
            }
            validateFilm(newFilm);

            Film oldFilm = films.get(newFilm.getId());
            oldFilm.setName(newFilm.getName());
            oldFilm.setDescription(newFilm.getDescription());
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
            oldFilm.setDuration(newFilm.getDuration());

            log.info("Фильм успешно обновлён: id={}, name='{}'", oldFilm.getId(), oldFilm.getName());
            return oldFilm;
        } catch (ValidationException | NotFoundException e) {
            log.error("Ошибка при обновлении фильма id={}: {}", newFilm.getId(), e.getMessage());
            throw e;
        }
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        Film film = films.get(filmId);
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Фильм не найден");
        }
        if (userStorage.getUsers().stream().noneMatch(u -> u.getId().equals(userId))) {
            throw new NotFoundException("Пользователь не найден");
        }
        film.addLike(userId);
        log.info("Фильм '{}' получил лайк от пользователя '{}'", filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        Film film = films.get(filmId);

        if (film == null) {
            throw new NotFoundException("Фильм не найден");
        }
        if (!film.getLikes().contains(userId)) {
            throw new NotFoundException("Лайк не найден");
        }
        film.removeLike(userId);

        log.info("Пользователь '{}' удалил лайк к фильму '{}'", userId, filmId);
    }

    @Override
    public int getLikesCount(Long filmId) {
        Film film = films.get(filmId);
        if (film == null) {
            throw new NotFoundException("Фильм не найден");
        }
        log.info("Количество лайков у фильма id={}", filmId);
        return film.getLikesCount();
    }

    @Override
    public List<Film> getTopFilms(int count) {
        if (count <= 0) {
            log.warn("Некорректное значение count: {}", count);
            throw new ValidationException("Значение count должно быть больше 0");
        }

        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikesCount(), f1.getLikesCount()))
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название не может быть пустым");
        }

        if (film.getDescription() == null || film.getDescription().length() > MAX_LENGTH_OF_DESCRIPTION) {
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_DATE)) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года");
        }

        if (film.getDuration() <= 0) {
            throw new ValidationException("продолжительность фильма должна быть положительным числом");
        }
    }

    private long getNextId() {
        return films.keySet().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0) + 1;
    }
}
