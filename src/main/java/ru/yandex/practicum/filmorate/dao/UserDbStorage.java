package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dao.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Repository
@Primary
@Qualifier("userDbStorage")
public class UserDbStorage implements UserStorage {
    private static final int CONFIRMED_STATUS = 2;
    private static final String CREATE_QUERY = "INSERT INTO users (email,login,name,birthday) VALUES (?,?,?,?)";
    private static final String UPDATE_QUERY =
            "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
    private static final String GET_ID_QUERY = "SELECT * FROM users WHERE id = ?";
    private static final String GET_ALL_QUERY = "SELECT * FROM users";
    private static final String CREATE_FRIENDSHIP_QUERY =
            "INSERT INTO friends (senderUser_id, receiverUser_id, status) VALUES (?,?,?)";
    private static final String UPDATE_FRIENDSHIP_QUERY =
            "UPDATE friends SET status = ? WHERE senderUser_id =? AND receiverUser_id = ?";
    private static final String GET_USER_FRIENDS_QUERY =
            "SELECT receiverUser_id FROM friends WHERE senderUser_id = ? " +
                    "UNION SELECT senderUser_id FROM friends WHERE receiverUser_id = ? AND status = 2";
    private static final String DELETE_FRIENDSHIP_QUERY =
            "DELETE FROM friends WHERE senderUser_id = ? AND receiverUser_id = ?";
    private static final String GET_FRIENDS_QUERY = "SELECT receiverUser_id FROM friends WHERE senderUser_id = ? " +
            "UNION SELECT senderUser_id FROM friends WHERE receiverUser_id = ? AND status = ?";
    private static final String DELETE_USER_BY_ID = "DELETE FROM users WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public User getUserById(Long id) {
        try {
            User user = jdbcTemplate.queryForObject(GET_ID_QUERY, new UserRowMapper(), id);
            if (user != null) {
                user.setFriends(loadFriends(user.getId()));
            }
            return user;
        } catch (DataAccessException e) {
            log.warn("Такого пользователя нет в списке!");
            throw new NotFoundException("Такого юзера нет в списке! " + e.getMessage());
        }
    }

    @Override
    public Collection<User> getAllUsers() {
        List<User> users = jdbcTemplate.query(GET_ALL_QUERY, new UserRowMapper());
        for (User user : users) {
            user.setFriends(loadFriends(user.getId()));
        }
        return users;
    }

    private Set<Long> loadFriends(Long userId) {
        return new HashSet<>(jdbcTemplate.queryForList(GET_USER_FRIENDS_QUERY, Long.class, userId, userId));
    }

    @Override
    public User createUser(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(CREATE_QUERY, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setDate(4, Date.valueOf(user.getBirthday()));
            return stmt;
        }, keyHolder);
        user.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return user;
    }

    @Override
    public User updateUser(User user) {
        if (getUserById(user.getId()) != null) {
            jdbcTemplate.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(UPDATE_QUERY);
                stmt.setString(1, user.getEmail());
                stmt.setString(2, user.getLogin());
                stmt.setString(3, user.getName());
                stmt.setDate(4, Date.valueOf(user.getBirthday()));
                stmt.setLong(5, user.getId());
                return stmt;
            });
            return user;
        } else {
            log.warn("Такого пользователя нет в списке!");
            throw new NotFoundException("Такого пользователя нет в списке!");
        }
    }

    @Override
    public User createFriendship(long user1Id, long user2Id) {
        jdbcTemplate.update(CREATE_FRIENDSHIP_QUERY, user1Id, user2Id, 1);
        return getUserById(user1Id);
    }

    @Override
    public User deleteFriendship(long id, long friendId) {
        jdbcTemplate.update(DELETE_FRIENDSHIP_QUERY, id, friendId);
        return getUserById(id);
    }

    @Override
    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(GET_ID_QUERY, new UserRowMapper(), id);
        if (users.isEmpty()) return Optional.empty();
        User user = users.get(0);
        user.setFriends(loadFriends(id));
        return Optional.of(user);
    }

    @Override
    public void deleteUser(Long userId) {
        int rowsDeleted = jdbcTemplate.update(DELETE_USER_BY_ID, userId);
        if (rowsDeleted == 0) {
            log.error("Пользователь с ID={} не найден", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }

    @Override
    public Collection<User> getFriends(Long id) {
        List<Long> friendIds = jdbcTemplate.queryForList(GET_FRIENDS_QUERY, Long.class, id, id, CONFIRMED_STATUS);
        return friendIds.stream().map(this::getUserById).collect(Collectors.toList());
    }

    @Override
    public Collection<User> getCommonFriends(Long id, Long otherId) {
        Collection<User> myFriends = getFriends(id);
        Collection<User> otherFriends = getFriends(otherId);
        return myFriends.stream()
                .filter(otherFriends::contains)
                .collect(Collectors.toList());
    }
}
