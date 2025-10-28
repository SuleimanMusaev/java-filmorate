package ru.yandex.practicum.filmorate.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryUserStorage implements UserStorage {
    private static final Logger log = LoggerFactory.getLogger(InMemoryUserStorage.class);
    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friends = new HashMap<>();

    @Override
    public Collection<User> getUsers() {
        log.info("GET-запрос: получение списка пользователей ({} шт)", users.size());
        return new ArrayList<>(users.values());
    }

    @Override
    public User create(User user) {
        log.info("Создание пользователя...: {}", user.getName());
        try {
            validateUser(user);
            user.setId(getNextId());
            users.put(user.getId(), user);

            log.info("Пользователь успешно создан id={}, name={}", user.getId(), user.getName());
            return user;

        } catch (ValidationException e) {
            log.warn("Ошибка при создании пользователя '{}': {}", user.getName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public User update(User newUser) {
        log.info("Попытка обновить пользователя id={}, name={}", newUser.getId(), newUser.getName());
        try {
            if (newUser.getId() == null || !users.containsKey(newUser.getId())) {
                log.warn("Ошибка: пользователь с id={} не найден", newUser.getId());
                throw new NotFoundException("Пользователь с id=" + newUser.getId() + " не найден");
            }
            validateUser(newUser);

            User oldUser = users.get(newUser.getId());
            oldUser.setEmail(newUser.getEmail());
            oldUser.setLogin(newUser.getLogin());
            oldUser.setName(newUser.getName());
            oldUser.setBirthday(newUser.getBirthday());

            log.info("Пользователь успешно обновлен: id={} имя='{}'", oldUser.getId(), oldUser.getName());
            return oldUser;
        } catch (ValidationException | NotFoundException e) {
            log.warn("Ошибка при обновлении пользователя с именем={}: {}", newUser.getName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public void delete(User user) {
        log.info("Удаление пользователя...: {}", user.getName());
        try {
            if (user.getId() == null || !users.containsKey(user.getId())) {
                log.warn("Ошибка: пользователь с именем={} не найден", user.getName());
                throw new NotFoundException("Пользователь с именем = " + user.getName() + " не найден");
            }

            users.remove(user.getId());
            log.info("Пользователь успешно удален: id={}, name='{}'", user.getId(), user.getName());
        } catch (ValidationException | NotFoundException e) {
            log.warn("Ошибка при удалении пользователя id={}: {}", user.getId(), e.getMessage());
            throw e;
        }
    }

    public void addFriend(Long userId, Long friendId) {
        if (!users.containsKey(userId) || !users.containsKey(friendId)) {
            throw new NotFoundException("Один из пользователей не найден");
        }

        friends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        friends.computeIfAbsent(friendId, k -> new HashSet<>()).add(userId);

        log.info("Пользователи {} и {} теперь друзья", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        if (!users.containsKey(userId) || !users.containsKey(friendId)) {
            throw new NotFoundException("Один из пользователей не найден");
        }

        friends.getOrDefault(userId, new HashSet<>()).remove(friendId);
        friends.getOrDefault(friendId, new HashSet<>()).remove(userId);

        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        Set<Long> friendsIds = friends.getOrDefault(userId, new HashSet<>());
        return friendsIds.stream()
                .map(users::get)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        if (!users.containsKey(userId) || !users.containsKey(otherId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        Set<Long> userFriends = friends.getOrDefault(userId, new HashSet<>());
        Set<Long> otherFriends = friends.getOrDefault(otherId, new HashSet<>());

        Set<Long> commonIds = new HashSet<>(userFriends);
        commonIds.retainAll(otherFriends);

        return commonIds.stream()
                .map(users::get)
                .collect(Collectors.toList());
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
            log.info("Имя для отображения пустое — установлен логин '{}' как имя пользователя", user.getLogin());
        }
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }
    }

    private long getNextId() {
        return users.keySet().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0) + 1;
    }
}
