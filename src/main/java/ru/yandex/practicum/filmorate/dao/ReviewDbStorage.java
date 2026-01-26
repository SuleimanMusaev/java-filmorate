package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Repository
@Qualifier("reviewDbStorage")
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {
    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_SELECT = """
            SELECT r.review_id, r.content, r.is_positive, r.user_id, r.film_id,
            COALESCE(SUM(CASE WHEN rl.is_like = true THEN 1 WHEN rl.is_like = false THEN -1 ELSE 0 END), 0) as useful
            FROM reviews r
            LEFT JOIN review_likes rl ON r.review_id = rl.review_id
            """;

    private static final String FIND_BY_ID_QUERY = BASE_SELECT + " WHERE r.review_id = ? GROUP BY r.review_id";

    private static final String FIND_ALL_QUERY = BASE_SELECT + " GROUP BY r.review_id ORDER BY useful DESC LIMIT ?";

    private static final String FIND_BY_FILM_QUERY = BASE_SELECT +
            " WHERE r.film_id = ? GROUP BY r.review_id ORDER BY useful DESC LIMIT ?";

    private static final String INSERT_QUERY =
            "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";

    private static final String UPDATE_QUERY =
            "UPDATE reviews SET content = ?, is_positive = ? WHERE review_id = ?";

    private static final String DELETE_QUERY = "DELETE FROM reviews WHERE review_id = ?";

    private static final String UPSERT_LIKE_QUERY =
            "MERGE INTO review_likes (review_id, user_id, is_like) KEY(review_id, user_id) VALUES (?, ?, ?)";

    private static final String DELETE_LIKE_QUERY = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ?";

    @Override
    public Review addReview(Review review) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(INSERT_QUERY, new String[]{"review_id"});
            stmt.setString(1, review.getContent());
            stmt.setBoolean(2, review.getIsPositive());
            stmt.setLong(3, review.getUserId());
            stmt.setLong(4, review.getFilmId());
            return stmt;
        }, keyHolder);
        review.setReviewId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return review;
    }

    @Override
    public Review updateReview(Review review) {
        int updated = jdbcTemplate.update(UPDATE_QUERY,
                review.getContent(),
                review.getIsPositive(),
                review.getReviewId());

        if (updated == 0) {
            throw new NotFoundException("Отзыв с id " + review.getReviewId() + " не найден");
        }
        return getReviewById(review.getReviewId());
    }

    @Override
    public void deleteReview(Long id) {
        if (jdbcTemplate.update(DELETE_QUERY, id) == 0) {
            throw new NotFoundException("Отзыв не найден");
        }
    }

    @Override
    public Review getReviewById(Long id) {
        try {
            return jdbcTemplate.queryForObject(FIND_BY_ID_QUERY, this::mapRowToReview, id);
        } catch (EmptyResultDataAccessException e) {
            log.error("Отзыв с id={} не найден", id);
            throw new NotFoundException("Отзыв с id " + id + " не найден");
        }
    }

    @Override
    public List<Review> getAllReviews(int count) {
        return jdbcTemplate.query(FIND_ALL_QUERY, this::mapRowToReview, count);
    }

    @Override
    public List<Review> getReviewsByFilmId(Long filmId, int count) {
        return jdbcTemplate.query(FIND_BY_FILM_QUERY, this::mapRowToReview, filmId, count);
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        jdbcTemplate.update(UPSERT_LIKE_QUERY, reviewId, userId, true);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        jdbcTemplate.update(UPSERT_LIKE_QUERY, reviewId, userId, false);
    }

    @Override
    public void deleteLike(Long reviewId, Long userId) {
        jdbcTemplate.update(DELETE_LIKE_QUERY, reviewId, userId);
    }

    @Override
    public void deleteDislike(Long reviewId, Long userId) {
        deleteLike(reviewId, userId);
    }

    private Review mapRowToReview(ResultSet rs, int rowNum) throws SQLException {
        return Review.builder()
                .reviewId(rs.getLong("review_id"))
                .content(rs.getString("content"))
                .isPositive(rs.getBoolean("is_positive"))
                .userId(rs.getLong("user_id"))
                .filmId(rs.getLong("film_id"))
                .useful(rs.getInt("useful"))
                .build();
    }
}
