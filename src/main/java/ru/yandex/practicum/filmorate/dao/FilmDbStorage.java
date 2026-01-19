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
    private static final String DELETE_FILM_RATING_QUERY =
            "DELETE FROM films_rating WHERE films_id = ?";
    private static final String DELETE_FILM_GENRES_QUERY =
            "DELETE FROM films_genre WHERE films_id = ?";
    private static final String GET_DIRECTORS_BY_FILM_QUERY =
            "SELECT d.id, d.name FROM director d " +
                    "JOIN film_director fd ON d.id = fd.director_id " +
                    "WHERE fd.film_id = ? ORDER BY d.name";


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
            throw new DatabaseException("Ошибка базы данных при получении фильма: " + e.getMessage());
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

        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            saveFilmDirectors(film.getId(), film.getDirectors());
        }

        return getFilmById(film.getId());
    }

    @Override
    @Transactional
    public Film updateFilm(Film film) {
        // Проверяем существование фильма
        getFilmById(film.getId());

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(UPDATE_QUERY);
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            stmt.setLong(5, film.getId());
            return stmt;
        });

        // Обновляем MPA
        jdbcTemplate.update(DELETE_FILM_RATING_QUERY, film.getId());
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            jdbcTemplate.update(INSERT_FILM_RATINGS_BY_ID_QUERY, film.getId(), film.getMpa().getId());
        }

        // Обновляем жанры
        jdbcTemplate.update(DELETE_FILM_GENRES_QUERY, film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Object[]> batch = film.getGenres().stream()
                    .map(g -> new Object[]{film.getId(), g.getId()})
                    .toList();
            jdbcTemplate.batchUpdate(INSERT_FILM_GENRES_QUERY, batch);
        }

        // Обновляем режиссеров
        deleteFilmDirectors(film.getId());
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            saveFilmDirectors(film.getId(), film.getDirectors());
        }

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

    @Override
    public void deleteFilm(Long filmId) {
        String deleteFilmSql = "DELETE FROM films WHERE id = ?";
        int rowsDeleted = jdbcTemplate.update(deleteFilmSql, filmId);

        if (rowsDeleted == 0) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }
    }

    @Override
    public void saveFilmDirectors(Long filmId, List<Director> directors) {
        if (directors == null || directors.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO film_director (film_id, director_id) VALUES (?, ?)";

        jdbcTemplate.batchUpdate(sql, directors, directors.size(),
                (PreparedStatement ps, Director director) -> {
                    ps.setLong(1, filmId);
                    ps.setLong(2, director.getId());
                });
    }

    @Override
    public void deleteFilmDirectors(Long filmId) {
        String sql = "DELETE FROM film_director WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }

    @Override
    public void loadDirectorsForFilms(List<Film> films) {
        if (films == null || films.isEmpty()) {
            return;
        }

        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        String inSql = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = String.format(
                "SELECT fd.film_id, d.id, d.name " + // Исправлено: d.id вместо d.director_id
                        "FROM film_director fd " +
                        "JOIN director d ON fd.director_id = d.id " + // Исправлено: director вместо directors
                        "WHERE fd.film_id IN (%s) " +
                        "ORDER BY fd.film_id", inSql);

        Map<Long, List<Director>> directorsByFilmId = jdbcTemplate.query(sql, filmIds.toArray(),
                rs -> {
                    Map<Long, List<Director>> result = new HashMap<>();
                    while (rs.next()) {
                        Long filmId = rs.getLong("film_id");
                        Director director = new Director(
                                rs.getLong("id"),
                                rs.getString("name")
                        );

                        result.computeIfAbsent(filmId, k -> new ArrayList<>())
                                .add(director);
                    }
                    return result;
                });

        for (Film film : films) {
            List<Director> filmDirectors = directorsByFilmId.getOrDefault(film.getId(), new ArrayList<>());
            film.setDirectors(filmDirectors);
        }
    }

    @Override
    public List<Director> loadDirectors(Film film) {
        List<Director> directors = jdbcTemplate.query(
                GET_DIRECTORS_BY_FILM_QUERY,
                new DirectorRowMapper(),
                film.getId()
        );
        film.setDirectors(directors);
        return directors;
    }
    @Override
    public List<Film> findFilmsByDirectorId(Long directorId, String sortBy) {
        // Проверяем существование режиссера
        String checkDirectorSql = "SELECT COUNT(*) FROM director WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(checkDirectorSql, Integer.class, directorId);
        if (count == null || count == 0) {
            throw new NotFoundException("Режиссер с id=" + directorId + " не найден");
        }

        String sql;
        if ("likes".equals(sortBy)) {
            sql = "SELECT f.*, r.id AS rating_id, r.name AS rating_name, " +
                    "COUNT(fl.users_id) AS likes_count " +
                    "FROM films f " +
                    "JOIN film_director fd ON f.id = fd.film_id " +
                    "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
                    "LEFT JOIN rating r ON r.id = fr.rating_id " +
                    "LEFT JOIN films_likes fl ON f.id = fl.films_id " +
                    "WHERE fd.director_id = ? " +
                    "GROUP BY f.id, r.id, r.name " +
                    "ORDER BY COUNT(fl.users_id) DESC, f.id"; // Исправлено: COUNT()
        } else { // "year" по умолчанию
            sql = "SELECT f.*, r.id AS rating_id, r.name AS rating_name " +
                    "FROM films f " +
                    "JOIN film_director fd ON f.id = fd.film_id " +
                    "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
                    "LEFT JOIN rating r ON r.id = fr.rating_id " +
                    "WHERE fd.director_id = ? " +
                    "ORDER BY f.releaseDate";
        }

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), directorId);

        // Загружаем дополнительную информацию для каждого фильма
        for (Film film : films) {
            film.setGenres(loadGenres(film.getId()));
            film.setLikes(loadLikes(film.getId()));
            loadDirectors(film);
        }

        return films;
    }
}
