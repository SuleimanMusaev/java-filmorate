package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Event;

import java.util.List;

public interface EventStorage {
    void addEvent(Long userId, Long entityId, String eventType, String operation);
    List<Event> getUserFeed(Long userId);
}
