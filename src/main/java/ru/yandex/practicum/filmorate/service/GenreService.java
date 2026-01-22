package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreStorage genreStorage;

    public Genre createGenre(Genre genre) {
        return genreStorage.createGenre(genre);
    }

    public Genre getGenreById(Long id) {
        Genre genre = genreStorage.getGenreById(id);
        if (genre == null) {
            throw new NotFoundException("Жанр с id=" + id + " не найден");
        }
        return genre;
    }

    public Collection<Genre> getAllGenres() {
        return genreStorage.getAllGenres();
    }
}
