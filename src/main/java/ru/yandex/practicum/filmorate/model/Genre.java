package ru.yandex.practicum.filmorate.model;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id"})
public class Genre {
    private Long id;
    private String name;
}
