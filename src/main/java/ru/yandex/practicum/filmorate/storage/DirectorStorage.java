package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;

public interface DirectorStorage {
    Director findById(long id);

    List<Director> findAll();

    Director save(Director director);

    void deleteById(long id);

    List<Director> findDirectorsByFilmId(long filmId);

}
