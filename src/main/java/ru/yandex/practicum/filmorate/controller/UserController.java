package ru.yandex.practicum.filmorate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private final static Logger log = LoggerFactory.getLogger(UserController.class);
    Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> getUsers() {
        log.info("GET-запрос: получение списка пользователей ({} шт)", users.size());
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
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

    @PutMapping
    public User update(@RequestBody User newUser) {
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
