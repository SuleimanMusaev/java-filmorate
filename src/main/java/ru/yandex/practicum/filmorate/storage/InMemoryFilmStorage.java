package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private long idCounter = 0;

    @Override
    public Collection<Film> getAllFilms() {
        return films.values();
    }

    @Override
    public Film createFilm(Film film) {
        film.setId(Long.valueOf(++idCounter));
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        if (films.containsKey(film.getId())) {
            films.put(film.getId(), film);
            return film;
        } else {
            throw new NotFoundException("Фильм не найден");
        }
    }

    @Override
    public Film getFilmById(Long id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм не найден");
        }
        return films.get(id);
    }

    @Override
    public void deleteFilm(Long filmId) {
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Фильм не найден");
        }
        films.remove(filmId);
    }

    @Override
    public Film userLikesFilm(Long id, Long userId) {
        Film film = getFilmById(id);
        film.getLikes().add(userId);
        return film;
    }

    @Override
    public Film deleteLikesFilm(Long id, Long userId) {
        Film film = getFilmById(id);
        film.getLikes().remove(userId);
        return film;
    }

    @Override
    public Collection<Film> getPopularFilms(Integer count, Long genreId, Integer year) {
        return new ArrayList<>(films.values());
    }

    @Override
    public Collection<Film> getCommonFilms(Long userId, Long friendId) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Film> searchFilms(String query, String by) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Film> getRecommendations(Long userId) {
        return Collections.emptyList();
    }

    @Override
    public List<Film> findFilmsByDirectorId(Long directorId, String sortBy) {
        return Collections.emptyList();
    }

    @Override
    public void saveFilmDirectors(Long filmId, List<Director> directors) {
    }

    @Override
    public void deleteFilmDirectors(Long filmId) {
    }

    @Override
    public void loadDirectorsForFilms(List<Film> films) {
    }

    @Override
    public List<Director> loadDirectors(Film film) {
        return Collections.emptyList();
    }

    @Override
    public Optional<Film> findById(Long id) {
        return Optional.ofNullable(films.get(id));
    }
}
