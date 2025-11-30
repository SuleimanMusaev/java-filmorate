package ru.yandex.practicum.filmorate.model;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
public class Rating {
    private Long id;
    private String name;
}
