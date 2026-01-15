package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewStorage reviewStorage;
    private final UserService userService;
    private final FilmService filmService;

    public Review create(Review review) {
        userService.findById(review.getUserId());
        filmService.findById(review.getFilmId());
        return reviewStorage.create(review);
    }

    public Review update(Review review) {
        findById(review.getReviewId());
        return reviewStorage.update(review);
    }

    public void delete(Long id) {
        findById(id);
        reviewStorage.delete(id);
    }

    public Review findById(Long id) {
        return reviewStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с id " + id + " не найден"));
    }

    public List<Review> findAll(Long filmId, int count) {
        return reviewStorage.findByFilmId(filmId, count);
    }

    public void addLike(Long id, Long userId) {
        reviewStorage.addLike(id, userId);
    }

    public void addDislike(Long id, Long userId) {
        reviewStorage.addDislike(id, userId);
    }

    public void deleteLike(Long id, Long userId) {
        reviewStorage.deleteLikeOrDislike(id, userId);
    }
}
