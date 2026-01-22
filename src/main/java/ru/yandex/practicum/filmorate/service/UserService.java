package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User createUser(User user) {
        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        return userStorage.updateUser(user);
    }

    public User getUserById(Long id) {
        return userStorage.getUserById(id);
    }

    public User findById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    public void deleteUser(Long id) {
        userStorage.deleteUser(id);
    }

    public User makeFriendship(Long id, Long friendId) {
        userStorage.getUserById(id);
        userStorage.getUserById(friendId);
        userStorage.createFriendship(id, friendId);
        return userStorage.getUserById(id);
    }

    public User deleteFriendship(Long id, Long friendId) {
        userStorage.getUserById(id);
        userStorage.getUserById(friendId);
        userStorage.deleteFriendship(id, friendId);
        return userStorage.getUserById(id);
    }

    public Collection<User> listOfFriends(Long id) {
        userStorage.getUserById(id);
        return userStorage.getFriends(id);
    }

    public Collection<User> listOfCommonFriends(Long id, Long otherId) {
        userStorage.getUserById(id);
        userStorage.getUserById(otherId);

        return userStorage.getCommonFriends(id, otherId);
    }

    public Collection<Film> getRecommendations(Long userId) {
        return filmStorage.getRecommendations(userId);
    }
}
