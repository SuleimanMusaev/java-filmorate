package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
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
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;
    private final DirectorRowMapper directorRowMapper;

    @Override
    public Collection<Director> getAllDirectors() {
        String sql = "SELECT * FROM directors";
        return jdbcTemplate.query(sql, directorRowMapper);
    }

    @Override
    public Optional<Director> getDirectorById(Long id) {
        String sql = "SELECT * FROM directors WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, directorRowMapper, id));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Director createDirector(Director director) {
        String sql = "INSERT INTO directors (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, director.getName());
            return stmt;
        }, keyHolder);
        director.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return director;
    }

    @Override
    public Director updateDirector(Director director) {
        String sql = "UPDATE directors SET name = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, director.getName(), director.getId());
        if (rows == 0) {
            throw new NotFoundException("Режиссёр с id=" + director.getId() + " не найден.");
        }
        return director;
    }

    @Override
    public void deleteDirector(Long id) {
        String sql = "DELETE FROM directors WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        if (rows == 0) {
            throw new NotFoundException("Режиссёр с id=" + id + " не найден.");
        }
    }
}
