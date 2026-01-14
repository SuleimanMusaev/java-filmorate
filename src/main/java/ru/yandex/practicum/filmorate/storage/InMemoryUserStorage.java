package ru.yandex.practicum.filmorate.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Component("userInMemoryStorage")
public class InMemoryUserStorage implements UserStorage {
    private static final Logger log = LoggerFactory.getLogger(InMemoryUserStorage.class);
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public Collection<User> getAllUsers() {
        return users.values();
    }

    @Override
    public User getUserById(Long id) {
        return users.get(id);
    }

    @Override
    public User createUser(User user) {
        user.setId(getNextId());
        log.debug("Валидация пройдена.");
        if (user.getName() == null) {
            user.setName(user.getLogin());
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id должен быть указан!");
        }
        User existing = users.get(user.getId());
        if (existing == null) {
            throw new NotFoundException("Такого пользователя нет в списке!");
        }
        existing.setEmail(user.getEmail());
        existing.setLogin(user.getLogin());
        existing.setBirthday(user.getBirthday());
        if (user.getName() == null || user.getName().isBlank()) {
            existing.setName(user.getLogin());
            log.debug("Заменили имя на логин.");
        } else {
            existing.setName(user.getName());
        }
        users.put(existing.getId(), existing);
        return existing;
    }

    @Override
    public User createFriendship(long id, long friendId) {
        User user1 = users.get(id);
        User user2 = users.get(friendId);

        if (user1 == null || user2 == null) {
            throw new NotFoundException("Один из пользователей не найден");
        }

        user1.getFriends().add(friendId);
        user2.getFriends().add(id);
        return user1;
    }

    @Override
    public User deleteFriendship(long id, long friendId) {
        User user1 = users.get(id);
        User user2 = users.get(friendId);

        if (user1 == null || user2 == null) {
            throw new NotFoundException("Один из пользователей не найден");
        }

        user1.getFriends().remove(friendId);
        user2.getFriends().remove(id);
        return user1;
    }

    @Override
    public Collection<User> listOfFriends(long id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        if (user.getFriends() == null || user.getFriends().isEmpty()) {
            throw new NotFoundException("Список друзей пуст!");
        }
        return user.getFriends().stream()
                .map(userId -> users.get(userId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<User> listOfCommonFriends(Long id, Long otherId) {
        User u1 = users.get(id);      // Используем users.get()
        User u2 = users.get(otherId);

        if (u1 == null || u2 == null) {
            throw new NotFoundException("Один из пользователей не найден");
        }

        Set<Long> friends1 = new HashSet<>(u1.getFriends());
        friends1.retainAll(u2.getFriends());

        return friends1.stream()
                .map(userId -> users.get(userId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @Override
    public void deleteUser(Long userId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        users.remove(userId);
    }

    @Override
    public void deleteFriendships(Long userId) {
        for (User user : users.values()) {
            user.getFriends().remove(userId);
        }
    }
}
