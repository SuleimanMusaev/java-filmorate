package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    private Long reviewId;

    @NotBlank(message = "Текст отзыва не может быть пустым")
    private String content;

    @NotNull(message = "Тип отзыва (положительный/отрицательный) должен быть указан")
    private Boolean isPositive;

    @NotNull(message = "ID пользователя обязателен")
    private Long userId;

    @NotNull(message = "ID фильма обязателен")
    private Long filmId;

    @Builder.Default // без этого возникает ошибка при создании сеттеров
    private int useful = 0;
}
