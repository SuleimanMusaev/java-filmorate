package ru.yandex.practicum.filmorate.dao.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Rating;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class FilmRowMapper implements RowMapper<Film> {
    @Override
    public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));

        Date rd = rs.getDate("releaseDate");
        if (rd != null) {
            film.setReleaseDate(rd.toLocalDate());
        }

        film.setDuration(rs.getInt("duration"));

        Long ratingId = rs.getLong("rating_id");
        String ratingName = rs.getString("rating_name");
        if (ratingId != 0) { // 0 = нет значения
            Rating rating = new Rating(ratingId, ratingName);
            film.setMpa(rating);
        }

        return film;
    }
}
