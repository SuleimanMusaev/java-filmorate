package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class DirectorService {
    private final DirectorStorage directorStorage;

    public Collection<Director> getAllDirectors() {
        return directorStorage.getAllDirectors();
    }

    public Director getDirectorById(Long id) {
        return directorStorage.getDirectorById(id)
                .orElseThrow(() -> new NotFoundException("Режиссёр с id = " + id + " не найден"));
    }

    public Director createDirector(Director director) {
        if (director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссёра не может быть пустым");
        }
        return directorStorage.createDirector(director);
    }

    public Director updateDirector(Director director) {
        if (director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссёра не может быть пустым");
        }
        // Проверка на существование перед обновлением
        getDirectorById(director.getId());
        return directorStorage.updateDirector(director);
    }

    public void deleteDirector(Long id) {
        directorStorage.deleteDirector(id);
    }
}
