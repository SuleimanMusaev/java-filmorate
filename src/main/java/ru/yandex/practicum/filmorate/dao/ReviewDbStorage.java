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

@Repository
@Qualifier("reviewDbStorage")
@RequiredArgsConstructor
@Slf4j
public class ReviewDbStorage implements ReviewStorage {
    private final JdbcTemplate jdbcTemplate;

    private static final String SELECT_REVIEWS =
            "SELECT r.review_id, r.content, r.is_positive, r.user_id, r.film_id, " +
                    "COALESCE(SUM(CASE WHEN rl.is_like = true THEN 1 WHEN rl.is_like = false THEN -1 ELSE 0 END), 0) as useful " +
                    "FROM reviews r " +
                    "LEFT JOIN review_likes rl ON r.review_id = rl.review_id ";

    private static final String GROUP_BY = " GROUP BY r.review_id ";
    private static final String ORDER_BY = " ORDER BY useful DESC ";

    @Override
    public Review addReview(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"review_id"});
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
        String sql = "UPDATE reviews SET content = ?, is_positive = ? WHERE review_id = ?";
        int updated = jdbcTemplate.update(sql,
                review.getContent(),
                review.getIsPositive(),
                review.getReviewId());

        if (updated == 0) {
            log.warn("Review not found for update, reviewId={}", review.getReviewId());
            throw new NotFoundException("Отзыв с id=" + review.getReviewId() + " не найден");
        }
        return getReviewById(review.getReviewId());
    }

    @Override
    public void deleteReview(Long id) {
        String sql = "DELETE FROM reviews WHERE review_id = ?";
        int deleted = jdbcTemplate.update(sql, id);
        if (deleted == 0) {
            log.warn("Review not found for delete, reviewId={}", id);
            throw new NotFoundException("Отзыв с id=" + id + " не найден");
        }
    }

    @Override
    public Review getReviewById(Long id) {
        String sql = SELECT_REVIEWS + "WHERE r.review_id = ?" + GROUP_BY;
        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToReview, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Отзыв с id " + id + " не найден");
        }
    }

    @Override
    public List<Review> getAllReviews(int count) {
        String sql = SELECT_REVIEWS + GROUP_BY + ORDER_BY + "LIMIT ?";
        return jdbcTemplate.query(sql, this::mapRowToReview, count);
    }

    @Override
    public List<Review> getReviewsByFilmId(Long filmId, int count) {
        String sql = SELECT_REVIEWS + "WHERE r.film_id = ?" + GROUP_BY + ORDER_BY + "LIMIT ?";
        return jdbcTemplate.query(sql, this::mapRowToReview, filmId, count);
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        String sql = "MERGE INTO review_likes (review_id, user_id, is_like) KEY(review_id, user_id) VALUES (?, ?, true)";
        jdbcTemplate.update(sql, reviewId, userId);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        String sql = "MERGE INTO review_likes (review_id, user_id, is_like) KEY(review_id, user_id) VALUES (?, ?, false)";
        jdbcTemplate.update(sql, reviewId, userId);
    }

    @Override
    public void deleteLike(Long reviewId, Long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, reviewId, userId);
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
