package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:filmorate;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.sql.init.mode=always",
        "spring.jpa.hibernate.ddl-auto=none"
})
class CommonFilmsIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDb() {
        jdbcTemplate.update("DELETE FROM films_likes");
        jdbcTemplate.update("DELETE FROM films_genre");
        jdbcTemplate.update("DELETE FROM films_rating");
        jdbcTemplate.update("DELETE FROM friends");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    @DisplayName("GET /films/common возвращает общие лайкнутые фильмы, отсортированные по убыванию лайков")
    void shouldReturnCommonFilmsSortedByLikesDesc() throws Exception {
        long user1 = createUser("u1@mail.ru", "u1", "U1");
        long user2 = createUser("u2@mail.ru", "u2", "U2");
        long user3 = createUser("u3@mail.ru", "u3", "U3");

        long film1 = createFilm("Film 1", "d1", LocalDate.of(2000, 1, 1), 120, 1);
        long film2 = createFilm("Film 2", "d2", LocalDate.of(2001, 1, 1), 121, 1);

        like(film1, user1);
        like(film1, user2);
        like(film2, user1);
        like(film2, user2);

        like(film2, user3);

        String json = mockMvc.perform(get("/films/common")
                        .param("userId", String.valueOf(user1))
                        .param("friendId", String.valueOf(user2)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.isArray()).isTrue();

        List<Long> ids = List.of(
                root.get(0).get("id").asLong(),
                root.get(1).get("id").asLong()
        );

        assertThat(ids).containsExactly(film2, film1);
    }

    private long createUser(String email, String login, String name) throws Exception {
        String body = "{" +
                "\"email\":\"" + email + "\"," +
                "\"login\":\"" + login + "\"," +
                "\"name\":\"" + name + "\"," +
                "\"birthday\":\"1999-01-01\"" +
                "}";

        String json = mockMvc.perform(post("/users")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }

    private long createFilm(String name, String description, LocalDate releaseDate, int duration, long ratingId) throws Exception {
        String body = "{" +
                "\"name\":\"" + name + "\"," +
                "\"description\":\"" + description + "\"," +
                "\"releaseDate\":\"" + releaseDate + "\"," +
                "\"duration\":" + duration + "," +
                "\"mpa\":{\"id\":" + ratingId + "}" +
                "}";

        String json = mockMvc.perform(post("/films")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }

    private void like(long filmId, long userId) throws Exception {
        mockMvc.perform(put("/films/{id}/like/{userId}", filmId, userId))
                .andExpect(status().isOk());
    }
}
