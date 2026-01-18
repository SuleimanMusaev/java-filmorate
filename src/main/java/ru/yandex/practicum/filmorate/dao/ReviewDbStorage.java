package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Review create(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"review_id"});
            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setLong(3, review.getUserId());
            ps.setLong(4, review.getFilmId());
            return ps;
        }, keyHolder);
        review.setReviewId(keyHolder.getKey().longValue());
        return review;
    }

    @Override
    public Review update(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ? WHERE review_id = ?";
        jdbcTemplate.update(sql, review.getContent(), review.getIsPositive(), review.getReviewId());
        return findById(review.getReviewId()).get();
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM reviews WHERE review_id = ?", id);
    }

    @Override
    public Optional<Review> findById(Long id) {
        String sql = "SELECT * FROM reviews WHERE review_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeReview(rs), id).stream().findFirst();
    }

    @Override
    public List<Review> findByFilmId(Long filmId, int count) {
        String sql;
        if (filmId == null) {
            sql = "SELECT * FROM reviews ORDER BY useful DESC LIMIT ?";
            return jdbcTemplate.query(sql, (rs, rowNum) -> makeReview(rs), count);
        } else {
            sql = "SELECT * FROM reviews WHERE film_id = ? ORDER BY useful DESC LIMIT ?";
            return jdbcTemplate.query(sql, (rs, rowNum) -> makeReview(rs), filmId, count);
        }
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        // Сначала удаляем старую реакцию (если была), чтобы избежать ошибок дублирования
        deleteLikeOrDislike(reviewId, userId);

        // Теперь безопасно добавляем лайк
        jdbcTemplate.update("INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, true)", reviewId, userId);
        updateUseful(reviewId, 1);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        // Сначала удаляем старую реакцию
        deleteLikeOrDislike(reviewId, userId);

        // Безопасно добавляем дизлайк
        jdbcTemplate.update("INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, false)", reviewId, userId);
        updateUseful(reviewId, -1);
    }

    @Override
    public void deleteLikeOrDislike(Long reviewId, Long userId) {
        // Проверяем, была ли оценка, и корректируем рейтинг перед удалением
        jdbcTemplate.query("SELECT is_like FROM review_likes WHERE review_id = ? AND user_id = ?",
                (rs) -> {
                    // Если был лайк (true), то при удалении рейтинг уменьшаем (-1).
                    // Если был дизлайк (false), то при удалении рейтинг увеличиваем (+1).
                    int delta = rs.getBoolean("is_like") ? -1 : 1;
                    updateUseful(reviewId, delta);
                }, reviewId, userId);

        jdbcTemplate.update("DELETE FROM review_likes WHERE review_id = ? AND user_id = ?", reviewId, userId);
    }

    private void updateUseful(Long reviewId, int delta) {
        jdbcTemplate.update("UPDATE reviews SET useful = useful + ? WHERE review_id = ?", delta, reviewId);
    }

    private Review makeReview(ResultSet rs) throws SQLException {
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
