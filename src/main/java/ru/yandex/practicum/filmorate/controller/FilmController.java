package ru.yandex.practicum.filmorate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/films")
public class FilmController {
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);
    private static final int MAX_LENGTH_OF_DESCRIPTION = 200;
    private static final LocalDate MIN_DATE = LocalDate.of(1895, 12, 28);
    private final Map<Long, Film> films = new HashMap<>();

    @GetMapping
    public Collection<Film> getFilms() {
        log.info("GET-запрос: получение списка всех фильмов({} шт)", films.size());
        return films.values();
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        log.info("Создание фильма...: {}", film.getName());
        try {
            validateFilm(film);

            film.setId(getNextId());
            films.put(film.getId(), film);

            log.info("Фильм успешно создан: id={}, name='{}'", film.getId(), film.getName());
            return film;
        } catch (ValidationException e) {
            log.warn("Ошибка при создании фильма '{}': {}", film.getName(), e.getMessage());
            throw e;
        }
    }

    @PutMapping
    public Film update(@RequestBody Film newFilm) {
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
            log.warn("Ошибка при обновлении фильма id={}: {}", newFilm.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Если использовать кастомный валидатор, то нужно пройтись по всем полям класса?
     * И на каждое поле создать аннотацию? Не совсем понял этот момент, как использовать
     * проверку каждого поля, как например тут для имени, описании и тд.
     */
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
