package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

public interface ReviewStorage {
    Review addReview(Review review);

    Review updateReview(Review review);

    void deleteReview(Long id);

    Review getReviewById(Long id);

    List<Review> getAllReviews(int count);

    List<Review> getReviewsByFilmId(Long filmId, int count);

    void addLike(Long reviewId, Long userId);

    void addDislike(Long reviewId, Long userId);

    void deleteLike(Long reviewId, Long userId); // Удаляет и лайк, и дизлайк

    void deleteDislike(Long reviewId, Long userId);
}
