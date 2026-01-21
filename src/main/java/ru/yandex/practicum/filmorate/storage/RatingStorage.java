package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Rating;

import java.util.Collection;

public interface RatingStorage {
    Rating getRatingById(Long id);

    Collection<Rating> getAllRatings();
}
