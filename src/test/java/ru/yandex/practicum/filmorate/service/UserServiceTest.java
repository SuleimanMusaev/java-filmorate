package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

argThat(u->u.

getId().

equals(1L)&&...)

        package ru.yandex.practicum.filmorate.service;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserStorage userStorage;

    @Mock
    private FilmStorage filmStorage;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;
    private Film film;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L)
                .email("user1@example.com")
                .login("user1")
                .name("User One")
                .birthday(LocalDate.of(1990, 1, 1))
                .friends(new HashSet<>())
                .build();

        user2 = User.builder()
                .id(2L)
                .email("user2@example.com")
                .login("user2")
                .name("User Two")
                .birthday(LocalDate.of(1995, 5, 5))
                .friends(new HashSet<>())
                .build();

        film = Film.builder()
                .id(100L)
                .name("Inception")
                .description("Dream within a dream")
                .releaseDate(LocalDate.of(2010, 7, 16))
                .duration(148)
                .build();
    }

    @Test
    void getAllUsers_shouldReturnAllUsers_whenUsersExist() {
        List<User> users = Arrays.asList(user1, user2);
        when(userStorage.getAllUsers()).thenReturn(users);

        Collection<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2).containsExactlyInAnyOrderElementsOf(users);
        verify(userStorage, times(1)).getAllUsers();
    }

    @Test
    void createUser_shouldCreateUser_withValidUserData() {
        when(userStorage.createUser(any(User.class))).thenReturn(user1);

        User result = userService.createUser(user1);

        assertThat(result).isEqualTo(user1);
        verify(userStorage, times(1)).createUser(argThat(u ->
                u.getId().equals(1L) &&
                        "user1@example.com".equals(u.getEmail()) &&
                        "user1".equals(u.getLogin()) &&
                        "User One".equals(u.getName()) &&
                        u.getBirthday().isEqual(LocalDate.of(1990, 1, 1))
        ));
    }

    @Test
    void updateUser_shouldUpdateExistingUser_withValidData() {
        when(userStorage.updateUser(any(User.class))).thenReturn(user1);

        User result = userService.updateUser(user1);

        assertThat(result).isEqualTo(user1);
        verify(userStorage, times(1)).updateUser(argThat(u -> u.getId().equals(1L)));
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        when(userStorage.getUserById(1L)).thenReturn(user1);

        User result = userService.getUserById(1L);

        assertThat(result).isEqualTo(user1);
        verify(userStorage, times(1)).getUserById(1L);
    }

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        when(userStorage.findById(1L)).thenReturn(Optional.of(user1));

        User result = userService.findById(1L);

        assertThat(result).isEqualTo(user1);
        verify(userStorage, times(1)).findById(1L);
    }

    @Test
    void findById_shouldThrowNotFoundException_whenUserDoesNotExist() {
        when(userStorage.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с id 999 не найден");

        verify(userStorage, times(1)).findById(999L);
    }

    @Test
    void deleteUser_shouldCallStorageDelete_once() {
        userService.deleteUser(1L);

        verify(userStorage, times(1)).deleteUser(1L);
    }

    @Test
    void makeFriendship_shouldCreateFriendship_whenBothUsersExist() {
        when(userStorage.getUserById(1L)).thenReturn(user1);
        when(userStorage.getUserById(2L)).thenReturn(user2);

        userService.makeFriendship(1L, 2L);

        verify(userStorage, times(1)).createFriendship(1L, 2L);
        verify(userStorage, times(1)).getUserById(1L);
        verify(userStorage, times(1)).getUserById(2L);
    }

    @Test
    void makeFriendship_shouldThrowNotFoundException_whenFirstUserNotFound() {
        when(userStorage.getUserById(1L)).thenReturn(null);

        assertThatThrownBy(() -> userService.makeFriendship(1L, 2L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с id 1 не найден");

        verify(userStorage, never()).createFriendship(anyLong(), anyLong());
    }

    @Test
    void makeFriendship_shouldThrowNotFoundException_whenSecondUserNotFound() {
        when(userStorage.getUserById(1L)).thenReturn(user1);
        when(userStorage.getUserById(2L)).thenReturn(null);

        assertThatThrownBy(() -> userService.makeFriendship(1L, 2L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь с id 2 не найден");

        verify(userStorage, never()).createFriendship(anyLong(), anyLong());
    }

    @Test
    void deleteFriendship_shouldRemoveFriend_andCallStorage() {
        when(userStorage.getUserById(1L)).thenReturn(user1);

        userService.deleteFriendship(1L, 2L);

        verify(userStorage, times(1)).deleteFriendship(1L, 2L);
        verify(userStorage, times(1)).getUserById(1L);
    }

    @Test
    void listOfFriends_shouldReturnListOfFriends_whenFriendsExist() {
        when(userStorage.getFriends(1L)).thenReturn(Collections.singletonList(user2));

        Collection<User> result = userService.listOfFriends(1L);

        assertThat(result).hasSize(1).containsExactly(user2);
        verify(userStorage, times(1)).getFriends(1L);
    }

    @Test
    void listOfFriends_shouldReturnEmptyList_whenNoFriends() {
        when(userStorage.getFriends(1L)).thenReturn(Collections.emptyList());

        Collection<User> result = userService.listOfFriends(1L);

        assertThat(result).isEmpty();
        verify(userStorage, times(1)).getFriends(1L);
    }

    @Test
    void listOfCommonFriends_shouldReturnCommonFriends_whenExist() {
        User commonFriend = User.builder()
                .id(3L)
                .email("common@example.com")
                .login("common")
                .name("Common Friend")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
        when(userStorage.getCommonFriends(1L, 2L)).thenReturn(Collections.singletonList(commonFriend));

        Collection<User> result = userService.listOfCommonFriends(1L, 2L);

        assertThat(result).hasSize(1).containsExactly(commonFriend);
        verify(userStorage, times(1)).getCommonFriends(1L, 2L);
    }

    @Test
    void getRecommendations_shouldReturnRecommendedFilms_whenAvailable() {
        List<Film> films = Collections.singletonList(film);
        when(filmStorage.getRecommendations(1L)).thenReturn(films);

        Collection<Film> result = userService.getRecommendations(1L);

        assertThat(result).hasSize(1)
                .first()
                .extracting(Film::getName)
                .isEqualTo("Inception");
        verify(filmStorage, times(1)).getRecommendations(1L);
    }
}
