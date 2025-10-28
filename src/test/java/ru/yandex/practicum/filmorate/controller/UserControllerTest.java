package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {
//    private UserController controller;
//    private User user;
//
//    @BeforeEach
//    void setUp() {
//        controller = new UserController();
//        user = new User();
//        user.setEmail("@yandex.com");
//        user.setName("Test name");
//        user.setLogin("TestLogin");
//        user.setBirthday(LocalDate.of(2000, 1, 1));
//    }
//
//    @Test
//    @DisplayName("Корректный пользователь проходит валидацию без ошибок")
//    void validUserShouldPass() {
//        assertDoesNotThrow(() -> callValidateUser(user));
//    }
//
//    @Test
//    @DisplayName("Email без '@' вызывает ValidationException")
//    void invalidEmailShouldThrow() {
//        user.setEmail("yandex.com");
//        assertThrows(ValidationException.class, () -> callValidateUser(user));
//    }
//
//    @Test
//    @DisplayName("Логин с пробелом вызывает ValidationException")
//    void loginWithSpaceShouldThrow() {
//        user.setLogin("user name");
//        assertThrows(ValidationException.class, () -> callValidateUser(user));
//    }
//
//    @Test
//    @DisplayName("Будущая дата рождения вызывает ValidationException")
//    void futureBirthdayShouldThrow() {
//        user.setBirthday(LocalDate.now().plusDays(1));
//        assertThrows(ValidationException.class, () -> callValidateUser(user));
//    }
//
//    @Test
//    @DisplayName("Пустое имя заменяется логином")
//    void emptyNameReplacedWithLogin() {
//        user.setName("");
//        callValidateUser(user);
//        assertEquals(user.getLogin(), user.getName());
//    }
//
//    private void callValidateUser(User user) {
//        try {
//            Method method = UserController.class.getDeclaredMethod("validateUser", User.class);
//            method.setAccessible(true);
//            method.invoke(controller, user);
//        } catch (Exception e) {
//            if (e.getCause() instanceof ValidationException) {
//                throw (ValidationException) e.getCause();
//            }
//            throw new RuntimeException(e);
//        }
//    }
}