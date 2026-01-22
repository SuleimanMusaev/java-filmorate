package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Optional;

public interface UserStorage {
    Collection<User> getAllUsers();

    User createUser(User user);

    User updateUser(User user);

    User getUserById(Long id);

    Optional<User> findById(Long id);

    void deleteUser(Long userId);

    User createFriendship(long user1Id, long user2Id);

    User deleteFriendship(long id, long friendId);

    Collection<User> getFriends(Long id);

    Collection<User> getCommonFriends(Long id, Long otherId);
}
