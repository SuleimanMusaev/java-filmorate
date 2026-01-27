package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final EventStorage eventStorage;

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User createUser(User user) {
        normalizeUserName(user);
        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        normalizeUserName(user);
        return userStorage.updateUser(user);
    }

    public User getUserById(Long id) {
        return userStorage.getUserById(id);
    }

    public void deleteUser(Long id) {
        userStorage.deleteUser(id);
    }

    public User makeFriendship(Long id, Long friendId) {
        userStorage.getUserById(id);
        userStorage.getUserById(friendId);
        userStorage.createFriendship(id, friendId);
        eventStorage.addEvent(id, friendId, "FRIEND", "ADD");

        return userStorage.getUserById(id);
    }

    public User deleteFriendship(Long id, Long friendId) {
        userStorage.getUserById(id);
        userStorage.getUserById(friendId);
        userStorage.deleteFriendship(id, friendId);

        eventStorage.addEvent(id, friendId, "FRIEND", "REMOVE");

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

    private void normalizeUserName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
