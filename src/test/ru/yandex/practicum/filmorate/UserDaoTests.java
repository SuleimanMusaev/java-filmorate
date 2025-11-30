package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dao.UserDbStorage;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor
public class UserDaoTests {
    @Autowired
    JdbcTemplate jdbc;
    UserDbStorage storage;
    User user;

    @BeforeEach
    void setUp() {
        storage = new UserDbStorage(jdbc);
        user = new User();
        user.setId(1L);
        user.setEmail("example@yandex.com");
        user.setLogin("example_login");
        user.setName("example");
        user.setBirthday(LocalDate.now().minusYears(15));
        user.setFriends(new HashSet<>());
    }

    @Test
    void findUserById() {
        User createdUser = storage.createUser(user);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isGreaterThan(0);

        User foundUser = storage.getUserById(createdUser.getId());

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo(user.getEmail());
        assertThat(foundUser.getLogin()).isEqualTo(user.getLogin());
    }

    @Test
    public void testUpdateUser() {
        User createdUser = storage.createUser(user);

        User updatedInfo = new User();
        updatedInfo.setId(user.getId());
        updatedInfo.setEmail("changed@yandex.com");
        updatedInfo.setLogin("changed_login");
        updatedInfo.setName("Changed");
        updatedInfo.setBirthday(LocalDate.now().minusYears(20));
        storage.updateUser(updatedInfo);

        User foundUser = storage.getUserById(createdUser.getId());

        assertThat(foundUser.getEmail()).isEqualTo(updatedInfo.getEmail());
        assertThat(foundUser.getLogin()).isEqualTo(updatedInfo.getLogin());
        assertThat(foundUser.getName()).isEqualTo(updatedInfo.getName());
    }

    @Test
    public void testGetAllUsers() {
        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@yandex.com");
        user2.setLogin("user2_login");
        user2.setName("User2");
        user2.setBirthday(LocalDate.now().minusYears(18));

        storage.createUser(user);
        storage.createUser(user2);

        Collection<User> users = storage.getAllUsers();

        assertThat(users).isNotNull();
        assertThat(users).hasSize(2);
    }
}
