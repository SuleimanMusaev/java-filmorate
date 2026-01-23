package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dao.mappers.DirectorRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
@Qualifier("directorDbStorage")
public class DirectorDbStorage implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;

    private static final String CREATE_QUERY = "INSERT INTO director (name) VALUES (?)";
    private static final String GET_BY_ID_QUERY = "SELECT id, name FROM director WHERE id = ?";
    private static final String GET_ALL_QUERY = "SELECT * FROM director ORDER BY id";
    private static final String UPDATE_QUERY = "UPDATE director SET name = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM director WHERE id = ?";
    private static final String GET_BY_FILM_ID_QUERY =
            "SELECT d.id, d.name FROM director d " +
                    "JOIN film_director fd ON d.id = fd.director_id " +
                    "WHERE fd.films_id = ? ORDER BY d.name";

    @Override
    public Director save(Director director) {
        if (director.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(CREATE_QUERY, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, director.getName());
                return stmt;
            }, keyHolder);
            director.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
            return director;
        } else {
            int updated = jdbcTemplate.update(UPDATE_QUERY, director.getName(), director.getId());
            if (updated == 0) {
                throw new NotFoundException("Режиссер с id=" + director.getId() + " не найден");
            }
            return director;
        }
    }

    @Override
    public Director findById(long id) {
        try {
            return jdbcTemplate.queryForObject(GET_BY_ID_QUERY, new DirectorRowMapper(), id);
        } catch (DataAccessException e) {
            throw new NotFoundException("Режиссер с id=" + id + " не найден");
        }
    }

    @Override
    public List<Director> findAll() {
        return jdbcTemplate.query(GET_ALL_QUERY, new DirectorRowMapper());
    }

    @Override
    public void deleteById(long id) {
        findById(id);
        String deleteLinksSql = "DELETE FROM film_director WHERE director_id = ?";
        jdbcTemplate.update(deleteLinksSql, id);
        int deleted = jdbcTemplate.update(DELETE_QUERY, id);

    }

    @Override
    public List<Director> findDirectorsByFilmId(long filmId) {
        return jdbcTemplate.query(GET_BY_FILM_ID_QUERY, new DirectorRowMapper(), filmId);
    }
}
