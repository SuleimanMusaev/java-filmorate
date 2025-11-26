package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User getUserById(Long id) {
        if (userStorage.getUserById(id) == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        return userStorage.getUserById(id);
    }

    public User makeFriendship(long id, long friendId) {
        if (userStorage.getUserById(id) == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        if (userStorage.getUserById(friendId) == null) {
            throw new NotFoundException("Невозможно добавить в друзья несуществующего юзера!");
        }
        userStorage.createFriendship(id, friendId);
        return userStorage.getUserById(id);
    }

    public User deleteFriendship(long id, long friendId) {
        if (userStorage.getUserById(id) == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        if (userStorage.getUserById(friendId) == null) {
            throw new NotFoundException("Удаляемого из друзья юзера нет в списке!");
        }
        userStorage.deleteFriendship(id, friendId);
        return userStorage.getUserById(id);
    }

    public Collection<User> listOfFriends(long id) {
        if (userStorage.getUserById(id) == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        if (userStorage.getUserById(id).getFriends() == null) {
            throw new NotFoundException("Список друзей пуст!");
        }
        return userStorage.listOfFriends(id);
    }

    public Collection<User> listOfCommonFriends(Long id, Long otherId) {
        if (userStorage.getUserById(id) == null || userStorage.getUserById(otherId) == null) {
            throw new NotFoundException("Одного из юзеров нет в списке!");
        }
        if (userStorage.getUserById(id).getFriends() == null || userStorage.getUserById(otherId).getFriends() == null) {
            throw new NotFoundException("Один из списков друзей пуст!");
        }
        return userStorage.listOfCommonFriends(id, otherId);
    }

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User createUser(User user) {
        validateUser(user);
        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        validateUser(user);
        return userStorage.updateUser(user);
    }

    private void validateUser(User user) {

        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Invalid email");
        }

        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Invalid login");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Birthday cannot be in the future");
        }
    }
}
