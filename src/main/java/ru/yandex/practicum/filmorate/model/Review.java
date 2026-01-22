package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Review {
    private Long reviewId;

    @NotNull
    private String content;

    @NotNull
    private Boolean isPositive;

    @NotNull
    private Long userId;

    @NotNull
    private Long filmId;

    @Builder.Default
    private Integer useful = 0;
}
