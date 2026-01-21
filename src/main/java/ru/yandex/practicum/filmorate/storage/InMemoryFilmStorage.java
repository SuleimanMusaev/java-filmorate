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
        if (film.getReleaseDate().isBefore(Film.CINEMA_BIRTHDAY)) {
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
            if (film.getReleaseDate().isBefore(Film.CINEMA_BIRTHDAY)) {
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

    @Override
    public Collection<Film> getFilmsByDirector(Long directorId, String sortBy) {
        return List.of();
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
    public Collection<Film> searchFilms(String query, boolean searchByTitle, boolean searchByDirector) {
        String lowerQuery = query.toLowerCase();
        return getAllFilms().stream()
                .filter(film -> {
                    boolean isTitleMatch = searchByTitle && film.getName().toLowerCase().contains(lowerQuery);
                    boolean isDirectorMatch = searchByDirector && film.getDirectors().stream()
                            .anyMatch(director -> director.getName().toLowerCase().contains(lowerQuery));
                    return isTitleMatch || isDirectorMatch;
                })
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .toList();
    }
}
