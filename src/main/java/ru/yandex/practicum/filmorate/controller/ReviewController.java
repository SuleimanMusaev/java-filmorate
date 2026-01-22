package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.UpdateReviewRequest;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;
import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public Review addReview(@Valid @RequestBody Review review) {
        log.info("Добавление отзыва: {}", review);
        return reviewService.addReview(review);
    }

    @PutMapping
    public Review updateReviewWithIdInBody(@Valid @RequestBody Review review) {
        log.info("Обновление отзыва (ID в теле): {}", review);
        // Проверяем, что отзыв существует
        Review existingReview = reviewService.getReviewById(review.getReviewId());

        // Создаем обновленный отзыв, сохраняя полезность из существующего
        Review updatedReview = Review.builder()
                .reviewId(review.getReviewId())
                .content(review.getContent())
                .isPositive(review.getIsPositive())
                .userId(existingReview.getUserId())  // Берем из существующего
                .filmId(existingReview.getFilmId())  // Берем из существующего
                .useful(existingReview.getUseful())  // Берем из существующего
                .build();

        return reviewService.updateReview(updatedReview);
    }

    @DeleteMapping("/{id}")
    public void deleteReview(@PathVariable Long id) {
        log.info("Удаление отзыва {}", id);
        reviewService.deleteReview(id);
    }

    @GetMapping("/{id}")
    public Review getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id);
    }

    @GetMapping
    public List<Review> getReviews(@RequestParam(required = false) Long filmId,
                                   @RequestParam(defaultValue = "10") int count) {
        if (filmId == null) {
            return reviewService.getAllReviews(count);
        }
        return reviewService.getReviewsByFilmId(filmId, count);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        reviewService.addLike(id, userId);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public void addDislike(@PathVariable Long id, @PathVariable Long userId) {
        reviewService.addDislike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        reviewService.deleteLike(id, userId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public void deleteDislike(@PathVariable Long id, @PathVariable Long userId) {
        reviewService.deleteDislike(id, userId);
    }
}
