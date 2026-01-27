package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
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

@Slf4j
@Repository
@RequiredArgsConstructor
@Primary
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {
    private static final String CREATE_QUERY =
            "INSERT INTO films (name, description, releaseDate, duration) VALUES (?,?,?,?)";
    private static final String UPDATE_QUERY =
            "UPDATE films SET name = ?, description = ?, releaseDate = ?, duration = ? WHERE id = ?";
    private static final String GET_ID_QUERY =
            "SELECT f.*, r.id AS rating_id, r.name AS rating_name " +
                    "FROM films f " +
                    "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
                    "LEFT JOIN rating r ON fr.rating_id = r.id " +
                    "WHERE f.id = ?";
    private static final String GET_ALL_QUERY = "SELECT f.*, r.id AS rating_id, r.name AS rating_name " +
            "FROM films f " +
            "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
            "LEFT JOIN rating r ON fr.rating_id = r.id";
    private static final String INSERT_FILM_RATINGS_QUERY =
            "INSERT INTO films_rating (films_id, rating_id) VALUES (?, ?)";
    private static final String DELETE_FILM_RATINGS_QUERY =
            "DELETE FROM films_rating WHERE films_id = ?";
    private static final String INSERT_FILM_GENRES_QUERY =
            "INSERT INTO films_genre (films_id, genre_id) VALUES (?, ?)";
    private static final String DELETE_FILM_GENRES_QUERY =
            "DELETE FROM films_genre WHERE films_id = ?";
    private static final String INSERT_FILM_LIKES_QUERY =
            "INSERT INTO films_likes (films_id, users_id) VALUES (?, ?)";
    private static final String DELETE_FILM_LIKES_BY_ID_QUERY =
            "DELETE FROM films_likes WHERE films_id=? AND users_id=?";
    private static final String DELETE_FILM_RATING_QUERY =
            "DELETE FROM films_rating WHERE films_id = ?";
    private static final String GET_DIRECTORS_BY_FILM_QUERY =
            "SELECT d.id, d.name FROM director d " +
                    "JOIN film_director fd ON d.id = fd.director_id " +
                    "WHERE fd.films_id = ? ORDER BY d.name";
    private static final String INSERT_FILM_DIRECTORS_QUERY =
            "INSERT INTO film_director (films_id, director_id) VALUES (?, ?)";
    private static final String DELETE_FILM_DIRECTORS_QUERY =
            "DELETE FROM film_director WHERE films_id = ?";
    private static final String COMMON_FILMS_QUERY =
            "SELECT f.*, r.id AS rating_id, r.name AS rating_name, COUNT(fl_all.users_id) AS like_count " +
                    "FROM films f " +
                    "JOIN films_likes fl_user ON f.id = fl_user.films_id AND fl_user.users_id = ? " +
                    "JOIN films_likes fl_friend ON f.id = fl_friend.films_id AND fl_friend.users_id = ? " +
                    "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
                    "LEFT JOIN rating r ON fr.rating_id = r.id " +
                    "LEFT JOIN films_likes fl_all ON f.id = fl_all.films_id " +
                    "GROUP BY f.id, f.name, f.description, f.releaseDate, f.duration, r.id, r.name " +
                    "ORDER BY like_count DESC";
    private static final String POPULAR_FILMS_QUERY =
            "SELECT f.*, r.id AS rating_id, r.name AS rating_name " +
                    "FROM films f " +
                    "LEFT JOIN films_likes fl ON f.id = fl.films_id " +
                    "LEFT JOIN films_genre fg ON f.id = fg.films_id " +
                    "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
                    "LEFT JOIN rating r ON fr.rating_id = r.id " +
                    "WHERE (? IS NULL OR fg.genre_id = ?) " +
                    "  AND (? IS NULL OR YEAR(f.releaseDate) = ?) " +
                    "GROUP BY f.id, r.id, r.name " +
                    "ORDER BY COUNT(fl.users_id) DESC " +
                    "LIMIT ?";
    private static final String MOST_SIMILAR_USER_QUERY =
            "SELECT fl_other.users_id AS other_id, COUNT(*) AS common_count " +
                    "FROM films_likes fl_user " +
                    "JOIN films_likes fl_other ON fl_user.films_id = fl_other.films_id " +
                    "WHERE fl_user.users_id = ? AND fl_other.users_id <> ? " +
                    "GROUP BY fl_other.users_id " +
                    "ORDER BY common_count DESC " +
                    "LIMIT 1";
    private static final String RECOMMENDATIONS_QUERY =
            "SELECT f.*, r.id AS rating_id, r.name AS rating_name, COUNT(fl_all.users_id) AS like_count " +
                    "FROM films f " +
                    "JOIN films_likes fl_other ON f.id = fl_other.films_id AND fl_other.users_id = ? " +
                    "LEFT JOIN films_likes fl_user ON f.id = fl_user.films_id AND fl_user.users_id = ? " +
                    "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
                    "LEFT JOIN rating r ON fr.rating_id = r.id " +
                    "LEFT JOIN films_likes fl_all ON f.id = fl_all.films_id " +
                    "WHERE fl_user.users_id IS NULL " +
                    "GROUP BY f.id, f.name, f.description, f.releaseDate, f.duration, r.id, r.name " +
                    "ORDER BY like_count DESC";
    private static final String SEARCH_FILM_QUERY = "SELECT f.*, r.id AS rating_id, r.name AS rating_name " +
            "FROM films f " +
            "LEFT JOIN films_rating fr ON f.id = fr.films_id " +
            "LEFT JOIN rating r ON fr.rating_id = r.id " +
            "LEFT JOIN film_director fd ON f.id = fd.films_id " +
            "LEFT JOIN director d ON fd.director_id = d.id " +
            "LEFT JOIN films_likes fl ON f.id = fl.films_id ";
    private static final String LOAD_GENRE_QUERY = """
            SELECT g.id, g.name
            FROM genre g
            JOIN films_genre fg ON g.id = fg.genre_id
            WHERE fg.films_id = ?
            ORDER BY g.id
            """;
    private static final String LOAD_LIKES_QUERY = "SELECT users_id FROM films_likes WHERE films_id = ?";
    private static final String DELETE_FILM_QUERY = "DELETE FROM films WHERE id = ?";
    private static final String LOAD_DIRECTOR_FOR_FILMS_QUERY = "SELECT fd.films_id, d.id, d.name " +
            "FROM film_director fd " +
            "JOIN director d ON fd.director_id = d.id " +
            "WHERE fd.films_id IN (%s) " +
            "ORDER BY fd.films_id";
    private static final String CHECK_DIRECTOR_EXISTS_QUERY = "SELECT COUNT(*) FROM director WHERE id = ?";

    private static final String FIND_BY_DIRECTOR_SORTED_BY_LIKES = """
            SELECT f.*, r.id AS rating_id, r.name AS rating_name, COUNT(fl.users_id) AS likes_count
            FROM films f
            JOIN film_director fd ON f.id = fd.films_id
            LEFT JOIN films_rating fr ON f.id = fr.films_id
            LEFT JOIN rating r ON fr.rating_id = r.id
            LEFT JOIN films_likes fl ON f.id = fl.films_id
            WHERE fd.director_id = ?
            GROUP BY f.id, r.id, r.name
            ORDER BY COUNT(fl.users_id) DESC, f.id
            """;

    private static final String FIND_BY_DIRECTOR_SORTED_BY_YEAR = """
            SELECT f.*, r.id AS rating_id, r.name AS rating_name
            FROM films f
            JOIN film_director fd ON f.id = fd.films_id
            LEFT JOIN films_rating fr ON f.id = fr.films_id
            LEFT JOIN rating r ON fr.rating_id = r.id
            WHERE fd.director_id = ?
            ORDER BY f.releaseDate
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserDbStorage userDbStorage;
    private final RatingDbStorage ratingDbStorage;

    @Override
    public Film getFilmById(Long id) {
        try {
            Film film = jdbcTemplate.queryForObject(GET_ID_QUERY, new FilmRowMapper(), id);
            assert film != null;
            film.setGenres(loadGenres(id));
            film.setLikes(loadLikes(id));
            film.setDirectors(loadDirectors(film));
            return film;
        } catch (EmptyResultDataAccessException e) {
            log.error("Фильм с ID '{}' не найден", id);
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        } catch (DataAccessException e) {
            log.error("Ошибка базы данных...");
            throw new DatabaseException("Ошибка базы данных: " + e.getMessage());
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
            log.warn("Дата релиза — не раньше 28 декабря 1895 года!");
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года!");
        }
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            log.warn("У рейтинга должен быть id.");
            throw new ValidationException("У рейтинга должен быть id.");
        }
        ratingDbStorage.getRatingById(film.getMpa().getId());

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

        // Сохраняем рейтинг в отдельную таблицу
        jdbcTemplate.update(INSERT_FILM_RATINGS_QUERY, film.getId(), film.getMpa().getId());

        // Сохраняем жанры
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Object[]> batch = film.getGenres().stream()
                    .map(g -> {
                        if (g.getId() == null) {
                            log.warn("У жанра должен быть id.");
                            throw new ValidationException("У жанра должен быть id.");
                        }
                        return new Object[]{film.getId(), g.getId()};
                    })
                    .toList();
            jdbcTemplate.batchUpdate(INSERT_FILM_GENRES_QUERY, batch);
        }

        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            saveFilmDirectors(film.getId(), film.getDirectors());
        }
        return getFilmById(film.getId());
    }

    @Override
    @Transactional
    public Film updateFilm(Film film) {
        getFilmById(film.getId()); // Проверка существования

        jdbcTemplate.update(UPDATE_QUERY,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getId());

        // Обновляем рейтинг (удаляем старый, добавляем новый)
        jdbcTemplate.update(DELETE_FILM_RATINGS_QUERY, film.getId());
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            jdbcTemplate.update(INSERT_FILM_RATINGS_QUERY, film.getId(), film.getMpa().getId());
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
    public Collection<Film> searchFilms(String query, String by) {
        StringBuilder whereClause = new StringBuilder("WHERE ");
        List<Object> params = new ArrayList<>();
        String searchParam = "%" + query.toLowerCase() + "%";
        boolean searchByDirector = by.contains("director");
        boolean searchByTitle = by.contains("title");

        if (searchByDirector && searchByTitle) {
            whereClause.append("(LOWER(d.name) LIKE ? OR LOWER(f.name) LIKE ?) ");
            params.add(searchParam);
            params.add(searchParam);
        } else if (searchByDirector) {
            whereClause.append("LOWER(d.name) LIKE ? ");
            params.add(searchParam);
        } else if (searchByTitle) {
            whereClause.append("LOWER(f.name) LIKE ? ");
            params.add(searchParam);
        } else {
            return new ArrayList<>();
        }

        String finalQuery = SEARCH_FILM_QUERY + whereClause + "GROUP BY f.id ORDER BY COUNT(DISTINCT fl.users_id) DESC";
        List<Film> films = jdbcTemplate.query(finalQuery, new FilmRowMapper(), params.toArray());

        for (Film f : films) {
            f.setGenres(loadGenres(f.getId()));
            f.setLikes(loadLikes(f.getId()));
            loadDirectors(f);
        }
        return films;
    }

    @Override
    public Film userLikesFilm(Long id, Long userId) {
        getFilmById(id);
        userDbStorage.getUserById(userId);

        try {
            jdbcTemplate.update(INSERT_FILM_LIKES_QUERY, id, userId);
        } catch (DataAccessException e) {
            log.warn("Like already exists for film={}, user={}. Returning film.", id, userId);
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
                    userId, userId
            );
        } catch (DataAccessException e) {
            return List.of();
        }
        List<Film> films = jdbcTemplate.query(RECOMMENDATIONS_QUERY, new FilmRowMapper(), similarUserId, userId);
        for (Film f : films) {
            f.setGenres(loadGenres(f.getId()));
            f.setLikes(loadLikes(f.getId()));
            loadDirectors(f);
        }
        return films;
    }

    @Override
    public Collection<Film> getPopularFilms(Integer count, Long genreId, Integer year) {
        List<Film> films = jdbcTemplate.query(
                POPULAR_FILMS_QUERY,
                new FilmRowMapper(),
                genreId, genreId,
                year, year,
                count
        );
        for (Film film : films) {
            film.setGenres(loadGenres(film.getId()));
            film.setLikes(loadLikes(film.getId()));
            loadDirectors(film);
        }
        return films;
    }

    private Set<Genre> loadGenres(Long filmId) {
        List<Genre> genres = jdbcTemplate.query(LOAD_GENRE_QUERY, new GenreRowMapper(), filmId);
        return new LinkedHashSet<>(genres);
    }

    private Set<Long> loadLikes(Long filmId) {
        return new HashSet<>(jdbcTemplate.queryForList(
                LOAD_LIKES_QUERY,
                Long.class,
                filmId
        ));
    }

    @Override
    public void deleteFilm(Long filmId) {
        int rowsDeleted = jdbcTemplate.update(DELETE_FILM_QUERY, filmId);
        if (rowsDeleted == 0) {
            log.error("Фильм с ID '{}' не найден", filmId);
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }
    }

    @Override
    public void saveFilmDirectors(Long filmId, List<Director> directors) {
        if (directors == null || directors.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT_FILM_DIRECTORS_QUERY, directors, directors.size(),
                (PreparedStatement ps, Director director) -> {
                    ps.setLong(1, filmId);
                    ps.setLong(2, director.getId());
                });
    }

    @Override
    public void deleteFilmDirectors(Long filmId) {
        jdbcTemplate.update(DELETE_FILM_DIRECTORS_QUERY, filmId);
    }

    @Override
    public void loadDirectorsForFilms(List<Film> films) {
        if (films == null || films.isEmpty()) return;
        List<Long> filmIds = films.stream().map(Film::getId).toList();
        String inSql = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = String.format(LOAD_DIRECTOR_FOR_FILMS_QUERY, inSql);

        Map<Long, List<Director>> directorsByFilmId = jdbcTemplate.query(sql, rs -> {
            Map<Long, List<Director>> result = new HashMap<>();
            while (rs.next()) {
                Long filmId = rs.getLong("films_id");
                Director director = new Director(rs.getLong("id"), rs.getString("name"));
                result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(director);
            }
            return result;
        }, filmIds.toArray());

        for (Film film : films) {
            assert directorsByFilmId != null;
            film.setDirectors(directorsByFilmId.getOrDefault(film.getId(), new ArrayList<>()));
        }
    }

    @Override
    public List<Director> loadDirectors(Film film) {
        List<Director> directors = jdbcTemplate.query(GET_DIRECTORS_BY_FILM_QUERY, new DirectorRowMapper(), film.getId());
        film.setDirectors(directors);
        return directors;
    }

    @Override
    public List<Film> findFilmsByDirectorId(Long directorId, String sortBy) {
        // Проверка существования режиссера
        Integer count = jdbcTemplate.queryForObject(CHECK_DIRECTOR_EXISTS_QUERY, Integer.class, directorId);
        if (count == 0) {
            throw new NotFoundException("Режиссер с id=" + directorId + " не найден");
        }

        String sql = "likes".equals(sortBy) ? FIND_BY_DIRECTOR_SORTED_BY_LIKES : FIND_BY_DIRECTOR_SORTED_BY_YEAR;

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), directorId);

        films.forEach(this::fillFilmMetadata);

        return films;
    }

    private void fillFilmMetadata(Film film) {
        film.setGenres(loadGenres(film.getId()));
        film.setLikes(loadLikes(film.getId()));
        loadDirectors(film);
    }

    @Override
    public Optional<Film> findById(Long id) {
        try {
            return Optional.of(getFilmById(id));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }
}
