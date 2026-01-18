package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;

@Service
public class UserService {
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("filmDbStorage") FilmStorage filmStorage) {
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
    }

    public User getUserById(Long id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            throw new NotFoundException("Такого юзера нет в списке!");
        }
        return user;
    }

    public User makeFriendship(long id, long friendId) {
        getUserById(id);
        getUserById(friendId);
        return userStorage.createFriendship(id, friendId);
    }

    public User deleteFriendship(long id, long friendId) {
        getUserById(id);
        getUserById(friendId);
        return userStorage.deleteFriendship(id, friendId);
    }

    public Collection<User> listOfFriends(long id) {
        getUserById(id);
        return userStorage.listOfFriends(id);
    }

    public Collection<User> listOfCommonFriends(Long id, Long otherId) {
        getUserById(id);
        getUserById(otherId);
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

    public void deleteUser(Long userId) {
        getUserById(userId);
        userStorage.deleteUser(userId);
    }
}
