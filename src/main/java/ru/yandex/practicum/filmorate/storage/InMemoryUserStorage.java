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
        if (getUserById(id) == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        if (getUserById(friendId) == null) {
            throw new NotFoundException("Невозможно добавить в друзья несуществующего юзера!");
        }
        getUserById(id).getFriends().add(friendId);
        getUserById(friendId).getFriends().add(id);
        return getUserById(id);
    }

    @Override
    public User deleteFriendship(long id, long friendId) {
        if (getUserById(id) == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        if (getUserById(friendId) == null) {
            throw new NotFoundException("Удаляемого из друзья юзера нет в списке!");
        }
        getUserById(id).getFriends().remove(friendId);
        getUserById(friendId).getFriends().remove(id);
        return getUserById(id);
    }

    @Override
    public Collection<User> listOfFriends(long id) {
        if (getUserById(id) == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        if (getUserById(id).getFriends() == null) {
            throw new NotFoundException("Список друзей пуст!");
        }
        return getUserById(id).getFriends().stream()
                .map(friends -> getUserById(friends))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<User> listOfCommonFriends(Long id, Long otherId) {
        User u1 = getUserById(id);
        User u2 = getUserById(otherId);
        if (u1 == null || u2 == null) throw new NotFoundException("Одного из юзеров нет в списке!");
        Set<Long> friends1 = new HashSet<>(u1.getFriends()); // копия
        friends1.retainAll(u2.getFriends());
        return friends1.stream()
                .map(this::getUserById)
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
}
