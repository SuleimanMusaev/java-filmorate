package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dao.mappers.RatingRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Rating;
import ru.yandex.practicum.filmorate.storage.RatingStorage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
@Qualifier("ratingDbStorage")
public class RatingDbStorage implements RatingStorage {
    private final JdbcTemplate jdbce;

    private static final String CREATE_QUERY = "INSERT INTO rating (name) VALUES (?)";
    private static final String GET_ID_QUERY = "SELECT id, name FROM rating WHERE id = ?";
    private static final String GET_ALL_QUERY = "SELECT * FROM rating";

    @Override
    public Rating createRating(Rating rating) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbce.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(CREATE_QUERY, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, rating.getName());
            return stmt;
        }, keyHolder);
        rating.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return rating;
    }

    @Override
    public Rating getRatingById(Long id) {
        try {
            return jdbce.queryForObject(GET_ID_QUERY, new RatingRowMapper(), id);
        } catch (DataAccessException e) {
            throw new NotFoundException("Такого рейтинга нет! " + e.getMessage());
        }
    }

    @Override
    public Collection<Rating> getAllRatings() {
        return jdbce.query(GET_ALL_QUERY, new RatingRowMapper());
    }
}
