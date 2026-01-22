package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewStorage reviewStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final EventStorage eventStorage;

    public Review addReview(Review review) {
        if (review.getUserId() == null || review.getFilmId() == null) {
            throw new NotFoundException("Id пользователя и фильма не должны быть null");
        }

        userStorage.getUserById(review.getUserId());
        filmStorage.getFilmById(review.getFilmId());

        Review saved = reviewStorage.addReview(review);

        eventStorage.addEvent(review.getUserId(), saved.getReviewId(), "REVIEW", "ADD");

        return saved;
    }

    public Review updateReview(Review review) {
        Review updated = reviewStorage.updateReview(review);

        eventStorage.addEvent(review.getUserId(), updated.getReviewId(), "REVIEW", "UPDATE");

        return updated;
    }

    public void deleteReview(Long id) {
        Review review = reviewStorage.getReviewById(id);

        reviewStorage.deleteReview(id);

        eventStorage.addEvent(
                review.getUserId(), id, "REVIEW", "REMOVE"
        );
    }

    public Review getReviewById(Long id) {
        return reviewStorage.getReviewById(id);
    }

    public List<Review> getAllReviews(int count) {
        return reviewStorage.getAllReviews(count);
    }

    public List<Review> getReviewsByFilmId(Long filmId, int count) {
        return reviewStorage.getReviewsByFilmId(filmId, count);
    }

    public void addLike(Long reviewId, Long userId) {
        reviewStorage.addLike(reviewId, userId);
    }

    public void addDislike(Long reviewId, Long userId) {
        reviewStorage.addDislike(reviewId, userId);
    }

    public void deleteLike(Long reviewId, Long userId) {
        reviewStorage.deleteLike(reviewId, userId);
    }

    public void deleteDislike(Long reviewId, Long userId) {
        reviewStorage.deleteDislike(reviewId, userId);
    }
}
