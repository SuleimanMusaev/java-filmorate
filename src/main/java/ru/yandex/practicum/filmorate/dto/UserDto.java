package ru.yandex.practicum.filmorate.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserDto {
    Long id;
    String email;
    String login;
    String name;
    LocalDate birthday;
}
