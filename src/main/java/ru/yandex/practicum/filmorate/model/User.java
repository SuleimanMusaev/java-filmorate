package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;

    @NotBlank(message = "Емайл пользователя не может быть пустым")
    @NotNull(message = "Емайл пользователя не может быть пустым")
    private String email;

    @NotBlank(message = "Логин пользователя не может быть пустым")
    private String login;

    private String name;

    @PastOrPresent(message = "Дата рождения не может быть в будущем")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Builder.Default
    private Set<Long> friends = new HashSet<>();
}
