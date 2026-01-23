package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorService {
    private final DirectorStorage directorStorage;

    public Director findById(long id) {
        return directorStorage.findById(id);
    }

    public Director create(Director director) {
        validateDirector(director);
        director.setId(null);
        return directorStorage.save(director);
    }

    public Director update(Director director) {
        validateDirector(director);
        if (director.getId() == null) {
            throw new ValidationException("ID режиссера обязателен для обновления");
        }
        findById(director.getId());
        return directorStorage.save(director);
    }

    public List<Director> findAll() {
        return directorStorage.findAll();
    }

    public Director save(Director director) {
        validateDirector(director);
        return directorStorage.save(director);
    }

    public void deleteById(long id) {
        findById(id);
        directorStorage.deleteById(id);
    }

    public List<Director> findDirectorsByFilmId(long filmId) {
        return directorStorage.findDirectorsByFilmId(filmId);
    }

    private void validateDirector(Director director) {
        if (director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссера не может быть пустым");
        }
    }
}