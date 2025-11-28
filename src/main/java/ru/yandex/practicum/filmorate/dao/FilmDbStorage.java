package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dao.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dao.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.exception.DatabaseException;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final UserDbStorage userDbStorage;

    private static final String CREATE_QUERY =
            "INSERT INTO films (name,description,releaseDate,duration) VALUES (?,?,?,?)";
    private static final String UPDATE_QUERY =
            "UPDATE films SET name = ?, description = ?, releaseDate = ?, duration = ? WHERE id = ?";
    private static final String GET_ID_QUERY =
            "SELECT f.*, r.id AS rating_id, r.name AS rating_name " +
                    "FROM films f " +
                    "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
                    "LEFT JOIN rating r ON r.id = fr.rating_id " +
                    "WHERE f.id = ?";
    private static final String GET_ALL_QUERY =
            "SELECT f.*, r.id AS rating_id, r.name AS rating_name " +
                    "FROM films f " +
                    "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
                    "LEFT JOIN rating r ON r.id = fr.rating_id";
    private static final String INSERT_FILM_RATINGS_BY_ID_QUERY =
            "INSERT INTO films_rating (films_id, rating_id) VALUES (?, ?)";
    private static final String INSERT_FILM_GENRES_QUERY =
            "INSERT INTO films_genre (films_id, genre_id) VALUES (?, ?)";
    private static final String INSERT_FILM_LIKES_QUERY =
            "INSERT INTO films_likes (films_id, users_id) VALUES (?, ?)";
    private static final String DELETE_FILM_LIKES_BY_ID_QUERY =
            "DELETE FROM films_likes WHERE films_id=? AND users_id=?";


    @Override
    public Film getFilmById(Long id) {
        Film film;
        try {
            film = jdbcTemplate.queryForObject(GET_ID_QUERY, new FilmRowMapper(), id);
        } catch (DataAccessException e) {
            throw new DatabaseException("Такого фильма не существует! " + e.getMessage());
        }

        film.setGenres(loadGenres(id));
        film.setLikes(loadLikes(id));

        return film;
    }

    @Override
    public Collection<Film> getAllFilms() {
        List<Film> films = jdbcTemplate.query(GET_ALL_QUERY, new FilmRowMapper());

        for (Film f : films) {
            f.setGenres(loadGenres(f.getId()));
            f.setLikes(loadLikes(f.getId()));
        }

        return films;
    }

    @Override
    public Film createFilm(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года!");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(CREATE_QUERY, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            return stmt;
        }, keyHolder);

        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Object[]> batch = film.getGenres().stream()
                    .map(g -> {
                        if (g.getId() == null) {
                            throw new ValidationException("У жанра должен быть id.");
                        }
                        return new Object[]{film.getId(), g.getId()};
                    })
                    .toList();
            try {
                jdbcTemplate.batchUpdate(INSERT_FILM_GENRES_QUERY, batch);
            } catch (DataAccessException e) {
                throw new DatabaseException("Ошибка при сохранении жанров: " + e.getMessage());
            }
        }

        if (film.getMpa() != null) {
            if (film.getMpa().getId() == null) {
                throw new ValidationException("У рейтинга должен быть id.");
            }
            try {
                jdbcTemplate.update(INSERT_FILM_RATINGS_BY_ID_QUERY, film.getId(), film.getMpa().getId());
            } catch (DataAccessException e) {
                throw new DatabaseException("Такого рейтинга не существует! " + e.getMessage());
            }
        }
        return getFilmById(film.getId());
    }

    @Override
    public Film updateFilm(Film film) {
        try {
            jdbcTemplate.queryForObject(GET_ID_QUERY, new FilmRowMapper(), film.getId());
        } catch (DataAccessException e) {
            throw new NotFoundException("Такого фильма нет в списке!  " + e.getMessage());
        }
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(UPDATE_QUERY);
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            stmt.setLong(5, film.getId());
            return stmt;
        });
        return film;
    }

    @Override
    public Film userLikesFilm(Long id, Long userId) {
        getFilmById(id);
        userDbStorage.getUserById(userId);
        try {
            jdbcTemplate.update(INSERT_FILM_LIKES_QUERY, id, userId);
        } catch (DataAccessException e) {
            throw new DuplicateException(e.getMessage());
        }
        return getFilmById(id);
    }

    @Override
    public Film deleteLikesFilm(Long id, Long userId) {
        getFilmById(id);
        userDbStorage.getUserById(userId);
        jdbcTemplate.update(DELETE_FILM_LIKES_BY_ID_QUERY, id, userId);
        return getFilmById(id);
    }

    private Set<Genre> loadGenres(Long filmId) {
        List<Genre> genres = jdbcTemplate.query(
                "SELECT g.id, g.name FROM genre g " +
                        "JOIN films_genre fg ON g.id = fg.genre_id WHERE fg.films_id = ?",
                new GenreRowMapper(),
                filmId
        );
        genres.sort(Comparator.comparing(Genre::getId));
        return new LinkedHashSet<>(genres);
    }

    private Set<Long> loadLikes(Long filmId) {
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT users_id FROM films_likes WHERE films_id = ?",
                Long.class,
                filmId
        ));
    }
}
