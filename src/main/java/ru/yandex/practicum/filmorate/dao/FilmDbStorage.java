package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dao.mappers.DirectorRowMapper;
import ru.yandex.practicum.filmorate.dao.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.dao.mappers.GenreRowMapper;
import ru.yandex.practicum.filmorate.exception.DatabaseException;
import ru.yandex.practicum.filmorate.exception.DuplicateException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.dao.mappers.FilmRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final UserDbStorage userDbStorage;
    private final RatingDbStorage ratingDbStorage;

    private static final String CREATE_QUERY =
            "INSERT INTO films (name, description, release_date, duration, rating_id) VALUES (?,?,?,?,?)";

    private static final String UPDATE_QUERY =
            "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, rating_id = ? WHERE id = ?";

    private static final String GET_ID_QUERY =
            "SELECT f.*, r.name AS rating_name " +
                    "FROM films f " +
                    "LEFT JOIN rating r ON f.rating_id = r.id " +
                    "WHERE f.id = ?";

    private static final String GET_ALL_QUERY =
            "SELECT f.*, r.name AS rating_name " +
                    "FROM films f " +
                    "LEFT JOIN rating r ON f.rating_id = r.id";

    private static final String INSERT_FILM_GENRES_QUERY =
            "INSERT INTO films_genre (film_id, genre_id) VALUES (?, ?)";

    private static final String INSERT_FILM_LIKES_QUERY =
            "INSERT INTO films_likes (film_id, user_id) VALUES (?, ?)";

    private static final String DELETE_FILM_LIKES_BY_ID_QUERY =
            "DELETE FROM films_likes WHERE film_id=? AND user_id=?";

    @Override
    public Film getFilmById(Long id) {
        try {
            Film film = jdbcTemplate.queryForObject(GET_ID_QUERY, new FilmRowMapper(), id);
            film.setGenres(loadGenres(id));
            film.setLikes(loadLikes(id));
            film.setDirectors(loadDirectors(film));
            return film;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        } catch (DataAccessException e) {
            throw new NotFoundException("Такого фильма не существует! " + e.getMessage());
        }
    }

    @Override
    public Collection<Film> getAllFilms() {
        List<Film> films = jdbcTemplate.query(GET_ALL_QUERY, new FilmRowMapper());
        for (Film f : films) {
            f.setGenres(loadGenres(f.getId()));
            f.setLikes(loadLikes(f.getId()));
            loadDirectors(f);
        }
        return films;
    }

    @Override
    @Transactional
    public Film createFilm(Film film) {

        if (film.getReleaseDate() != null &&
                film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года!");
        }

        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("У рейтинга должен быть id.");
        }
        ratingDbStorage.getRatingById(film.getMpa().getId());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt =
                    connection.prepareStatement(CREATE_QUERY, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            if (film.getMpa() != null) {
                stmt.setLong(5, film.getMpa().getId());
            } else {
                stmt.setObject(5, null);
            }
            return stmt;
        }, keyHolder);

        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());

        // Сохраняем MPA рейтинг
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

        // Сохраняем жанры
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> uniqueGenres = new LinkedHashSet<>(film.getGenres());
            List<Object[]> batch = uniqueGenres.stream()
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
            film.setGenres(uniqueGenres);
        }

        return getFilmById(film.getId());
    }

    @Override
    @Transactional
    public Film updateFilm(Film film) {
        try {
            jdbcTemplate.queryForObject(GET_ID_QUERY, new FilmRowMapper(), film.getId());
        } catch (DataAccessException e) {
            throw new NotFoundException("Такого фильма нет в списке! " + e.getMessage());
        }

        jdbcTemplate.update("DELETE FROM films_genre WHERE film_id = ?", film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> uniqueGenres = new LinkedHashSet<>(film.getGenres());
            List<Object[]> batch = uniqueGenres.stream()
                    .map(g -> new Object[]{film.getId(), g.getId()})
                    .toList();
            jdbcTemplate.batchUpdate(INSERT_FILM_GENRES_QUERY, batch);
        }

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(UPDATE_QUERY);
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            if (film.getMpa() != null) {
                stmt.setLong(5, film.getMpa().getId());
            } else {
                stmt.setObject(5, null);
            }
            stmt.setLong(6, film.getId());
            return stmt;
        });

        return getFilmById(film.getId());
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

    @Override
    public Collection<Film> getCommonFilms(Long userId, Long friendId) {
        userDbStorage.getUserById(userId);
        userDbStorage.getUserById(friendId);

        List<Film> films = jdbcTemplate.query(COMMON_FILMS_QUERY, new FilmRowMapper(), userId, friendId);
        for (Film f : films) {
            f.setGenres(loadGenres(f.getId()));
            f.setLikes(loadLikes(f.getId()));
        }
        return films;
    }

    @Override
    public Collection<Film> getRecommendations(Long userId) {
        userDbStorage.getUserById(userId);

        Long similarUserId;
        try {
            similarUserId = jdbcTemplate.queryForObject(
                    MOST_SIMILAR_USER_QUERY,
                    (rs, rowNum) -> rs.getLong("other_id"),
                    userId,
                    userId
            );
        } catch (DataAccessException e) {
            return List.of();
        }

        List<Film> films = jdbcTemplate.query(RECOMMENDATIONS_QUERY, new FilmRowMapper(), similarUserId, userId);
        for (Film f : films) {
            f.setGenres(loadGenres(f.getId()));
            f.setLikes(loadLikes(f.getId()));
        }
        return films;
    }

    @Override
    public Collection<Film> getPopularFilms(Integer count, Long genreId, Integer year) {

        int limit = (count != null) ? count : 10;

        List<Film> films = jdbcTemplate.query(
                POPULAR_FILMS_QUERY,
                new FilmRowMapper(),
                genreId, genreId,
                year, year,
                limit
        );

        for (Film film : films) {
            film.setGenres(loadGenres(film.getId()));
            film.setLikes(loadLikes(film.getId()));
        }

        return films;
    }

    private Set<Genre> loadGenres(Long filmId) {
        String sql = "SELECT g.id, g.name FROM genre g " +
                "JOIN films_genre fg ON g.id = fg.genre_id WHERE fg.film_id = ?";
        List<Genre> genres = jdbcTemplate.query(sql, new GenreRowMapper(), filmId);
        genres.sort(Comparator.comparing(Genre::getId));
        return new LinkedHashSet<>(genres);
    }

    private Set<Long> loadLikes(Long filmId) {
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT user_id FROM films_likes WHERE film_id = ?",
                Long.class,
                filmId
        ));
    }

    @Override
    public Optional<Film> findById(Long id) {
        List<Film> films = jdbcTemplate.query(GET_ID_QUERY, new FilmRowMapper(), id);
        if (films.isEmpty()) {
            return Optional.empty();
        }
        Film film = films.get(0);
        film.setGenres(loadGenres(id));
        film.setLikes(loadLikes(id));
        return Optional.of(film);
    }
}
