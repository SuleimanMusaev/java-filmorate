package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.dto.mappers.UserMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @GetMapping
    public Collection<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable("id") long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public User createUser(@Valid @RequestBody UserDto userDto) {
        User user = UserMapper.mapToUser(userDto);
        return userService.createUser(user);
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody UserDto userDto) {
        User user = UserMapper.mapToUser(userDto);
        return userService.updateUser(user);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public User putFriends(@PathVariable("id") long id, @PathVariable("friendId") long friendId) {
        return userService.makeFriendship(id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public User deleteFriends(@PathVariable("id") long id, @PathVariable("friendId") long friendId) {
        return userService.deleteFriendship(id, friendId);
    }

    @GetMapping("/{id}/friends")
    public Collection<User> listFriends(@PathVariable("id") long id) {
        return userService.listOfFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public Collection<User> commonFriends(@PathVariable("id") long id, @PathVariable("otherId") long otherId) {
        return userService.listOfCommonFriends(id, otherId);
    }

    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }

    @GetMapping("/{id}/recommendations")
    public Collection<Film> getRecommendations(@PathVariable("id") long id) {
        return userService.getRecommendations(id);
    }
}
