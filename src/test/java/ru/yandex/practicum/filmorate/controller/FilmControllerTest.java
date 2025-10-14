package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {
    private FilmController controller;
    private Film film;

    @BeforeEach
    void setUp() {
        controller = new FilmController();
        film = new Film();
        film.setName("Test Film");
        film.setDescription("Good movie");
        film.setReleaseDate(LocalDate.of(2000, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
        film.setDuration(Duration.ofMinutes(120));
    }

    @Test
    @DisplayName("Валидный фильм проходит проверку без ошибок")
    void validFilmShouldPass() {
        assertDoesNotThrow(() -> callValidateFilm(film));
    }

    @Test
    @DisplayName("Пустое название вызывает ValidationException")
    void emptyNameShouldThrow() {
        film.setName("");
        assertThrows(ValidationException.class, () -> callValidateFilm(film));
    }

    @Test
    @DisplayName("Описание длиннее 200символов вызывает ValidationException")
    void tooLongDescriptionShouldThrow() {
        film.setDescription("A".repeat(201));
        assertThrows(ValidationException.class, () -> callValidateFilm(film));
    }

    @Test
    @DisplayName("Дата релиза раньше 28.12.1895 вызывает ValidationException")
    void tooOldReleaseDateShouldThrow() {
        film.setReleaseDate(LocalDate.of(1800, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
        assertThrows(ValidationException.class, () -> callValidateFilm(film));
    }

    @Test
    @DisplayName("Отрицательная длительность вызывает ValidationException")
    void negativeDurationShouldThrow() {
        film.setDuration(Duration.ofMinutes(-10));
        assertThrows(ValidationException.class, () -> callValidateFilm(film));
    }

    private void callValidateFilm(Film film) {
        try {
            Method method = FilmController.class.getDeclaredMethod("validateFilm", Film.class);
            method.setAccessible(true);
            method.invoke(controller, film);
        } catch (Exception e) {
            if (e.getCause() instanceof ValidationException) {
                throw (ValidationException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }
}