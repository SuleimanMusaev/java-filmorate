package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dao.mappers.RatingRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Rating;
import ru.yandex.practicum.filmorate.storage.RatingStorage;

import java.util.Collection;

@Repository
@RequiredArgsConstructor
@Qualifier("ratingDbStorage")
public class RatingDbStorage implements RatingStorage {
    private final JdbcTemplate jdbc;
    private static final String GET_ID_QUERY = "SELECT * FROM rating WHERE id = ?";
    private static final String GET_ALL_QUERY = "SELECT * FROM rating";

    @Override
    public Rating getRatingById(Long id) {
        try {
            return jdbc.queryForObject(GET_ID_QUERY, new RatingRowMapper(), id);
        } catch (DataAccessException e) {
            throw new NotFoundException("Такого рейтинга нет! " + e.getMessage());
        }
    }

    @Override
    public Collection<Rating> getAllRatings() {
        return jdbc.query(GET_ALL_QUERY, new RatingRowMapper());
    }
}
