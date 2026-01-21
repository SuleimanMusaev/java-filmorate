package ru.yandex.practicum.filmorate.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component("inMemoryFilmStorage")
public class InMemoryFilmStorage implements FilmStorage {
    private final UserStorage userStorage;
    private final Map<Long, Film> films = new HashMap<>();

    public InMemoryFilmStorage(@Qualifier("userInMemoryStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    private static final Logger log = LoggerFactory.getLogger(InMemoryFilmStorage.class);

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

    @Override
    public Collection<Film> getCommonFilms(Long userId, Long friendId) {
        List<Film> result = films.values().stream()
                .filter(f -> f.getLikes().contains(userId) && f.getLikes().contains(friendId))
                .sorted((a, b) -> Integer.compare(b.getLikes().size(), a.getLikes().size()))
                .toList();
        return result;
    }

    @Override
    public Collection<Film> getRecommendations(Long userId) {
        Set<Long> userLikes = films.values().stream()
                .filter(f -> f.getLikes().contains(userId))
                .map(Film::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (userLikes.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> commonCounts = new HashMap<>();
        for (Film film : films.values()) {
            if (!userLikes.contains(film.getId())) {
                continue;
            }
            for (Long liker : film.getLikes()) {
                if (liker.equals(userId)) {
                    continue;
                }
                commonCounts.merge(liker, 1, Integer::sum);
            }
        }

        Optional<Map.Entry<Long, Integer>> best = commonCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (best.isEmpty()) {
            return List.of();
        }

        Long similarUserId = best.get().getKey();

        List<Film> result = films.values().stream()
                .filter(f -> f.getLikes().contains(similarUserId) && !f.getLikes().contains(userId))
                .sorted((a, b) -> Integer.compare(b.getLikes().size(), a.getLikes().size()))
                .toList();

        return result;
    }

    private Long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @Override
    public void deleteFilm(Long filmId) {
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }
        films.remove(filmId);
    }


    @Override
    public void saveFilmDirectors(Long filmId, List<Director> directors) {
        throw new UnsupportedOperationException("Метод не поддерживается в in-memory реализации");
    }

    @Override
    public List<Director> loadDirectors(Film film) {
        return new ArrayList<>();
    }

    @Override
    public void loadDirectorsForFilms(List<Film> films) {
        // Ничего не делаем для in-memory
    }

    @Override
    public List<Film> findFilmsByDirectorId(Long directorId, String sortBy) {
        throw new UnsupportedOperationException("Метод не поддерживается в in-memory реализации");
    }

    @Override
    public void deleteFilmDirectors(Long filmId) {
        Film film = films.get(filmId);
        if (film == null) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }
        film.setDirectors(new ArrayList<>());
    }

    @Override
    public Collection<Film> searchFilms(String query, String by) {
        String lowerQuery = query.toLowerCase();
        return films.values().stream()
                .filter(film -> {
                    boolean match = false;
                    // Проверка по режиссеру (если список режиссеров не пуст)
                    if (by.contains("director")) {
                        match = film.getDirectors().stream()
                                .anyMatch(d -> d.getName().toLowerCase().contains(lowerQuery));
                    }
                    // Проверка по названию (через ИЛИ, если уже нашли по режиссеру - true останется)
                    if (by.contains("title")) {
                        match = match || film.getName().toLowerCase().contains(lowerQuery);
                    }
                    return match;
                })
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .collect(Collectors.toList());
    }
}
