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
class RecommendationsIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM film_director");
        jdbcTemplate.update("DELETE FROM friends");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.update("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    @DisplayName("GET /users/{id}/recommendations возвращает фильмы, лайкнутые самым похожим пользователем, которых нет у текущего")
    void shouldReturnRecommendationsFromMostSimilarUser() throws Exception {
        long user1 = createUser("u1@mail.ru", "u1", "U1");
        long user2 = createUser("u2@mail.ru", "u2", "U2");
        long user3 = createUser("u3@mail.ru", "u3", "U3");

        long film1 = createFilm("Film 1", "d1", LocalDate.of(2000, 1, 1), 120, 1);
        long film2 = createFilm("Film 2", "d2", LocalDate.of(2001, 1, 1), 121, 1);
        long film3 = createFilm("Film 3", "d3", LocalDate.of(2002, 1, 1), 122, 1);
        long film4 = createFilm("Film 4", "d4", LocalDate.of(2003, 1, 1), 123, 1);

        like(film1, user1);
        like(film2, user1);

        like(film1, user2);
        like(film2, user2);
        like(film3, user2);
        like(film4, user2);

        like(film4, user3);

        String json = mockMvc.perform(get("/users/{id}/recommendations", user1))
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

        assertThat(ids).containsExactly(film4, film3);
    }

    @Test
    @DisplayName("GET /users/{id}/recommendations возвращает пустой список, если у пользователя нет лайков")
    void shouldReturnEmptyWhenUserHasNoLikes() throws Exception {
        long user1 = createUser("u1@mail.ru", "u1", "U1");
        createUser("u2@mail.ru", "u2", "U2");

        String json = mockMvc.perform(get("/users/{id}/recommendations", user1))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.isArray()).isTrue();
        assertThat(root).isEmpty();
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
