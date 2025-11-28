package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dao.FilmDbStorage;
import ru.yandex.practicum.filmorate.dao.UserDbStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Rating;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor
public class FilmDaoTests {
    @Autowired
    JdbcTemplate jdbc;
    FilmDbStorage filmDbStorage;
    UserDbStorage userDbStorage;
    Film film;

    @BeforeEach
    void setUp() {
        filmDbStorage =new FilmDbStorage(jdbc, userDbStorage);
        film = new Film();
        film.setId(1L);
        film.setName("example");
        film.setDescription("example_description");
        film.setReleaseDate(LocalDate.now().minusYears(50));
        film.setDuration(150);
        film.setLikes(new HashSet<>());
        film.setGenres(new HashSet<>());
        Rating rating = new Rating(4L, "R");
        film.setMpa(rating);
    }

    @Test
    void findUserById() {
        Film createdFilm = filmDbStorage.createFilm(film);

        assertThat(createdFilm).isNotNull();
        assertThat(createdFilm.getId()).isGreaterThan(0);

        Film foundFilm = filmDbStorage.getFilmById(createdFilm.getId());

        assertThat(foundFilm).isNotNull();
        assertThat(foundFilm.getName()).isEqualTo(film.getName());
        assertThat(foundFilm.getName()).isEqualTo(film.getName());
    }

    @Test
    public void testUpdateUser() {
        Film createdFilm = filmDbStorage.createFilm(film);

        Film updatedInfo = new Film();
        updatedInfo.setId(film.getId());
        updatedInfo.setName("changed");
        updatedInfo.setDescription("changed_description");
        updatedInfo.setDuration(138);
        updatedInfo.setReleaseDate(LocalDate.now().minusYears(20).minusMonths(6));
        filmDbStorage.updateFilm(updatedInfo);

        Film foundFilm = filmDbStorage.getFilmById(createdFilm.getId());

        assertThat(foundFilm.getName()).isEqualTo(updatedInfo.getName());
        assertThat(foundFilm.getId()).isEqualTo(updatedInfo.getId());
    }

    @Test
    public void testGetAllFilms() {
        Film film2 = new Film();
        film2.setId(1L);
        film2.setName("example");
        film2.setDescription("example_description");
        film2.setReleaseDate(LocalDate.now().minusYears(10));
        film2.setDuration(150);
        film2.setLikes(new HashSet<>());
        film2.setGenres(new HashSet<>());
        Rating rating = new Rating(1L, "G");
        film.setMpa(rating);

        filmDbStorage.createFilm(film);
        filmDbStorage.createFilm(film2);

        Collection<Film> films = filmDbStorage.getAllFilms();

        assertThat(films).isNotNull();
        assertThat(films).hasSize(2);
    }
}
