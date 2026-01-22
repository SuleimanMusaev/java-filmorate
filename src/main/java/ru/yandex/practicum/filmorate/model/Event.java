package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private Long eventId;
    private Long userId;
    private Long entityId;
    private String eventType;
    private String operation;
    private Long timestamp;
}
