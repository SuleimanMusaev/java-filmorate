package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.UserDbStorage;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventStorage eventStorage;
    private final UserDbStorage userDbStorage;

    void addEvent(Long userId, Long entityId, String eventType, String operation) {
        eventStorage.addEvent(userId, entityId, eventType, operation);
    }

    public List<Event> getUserFeed(Long userId) {
        userDbStorage.getUserById(userId);
        return eventStorage.getUserFeed(userId);
    }
}
