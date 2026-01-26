package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventStorage eventStorage;
    private final UserStorage userStorage; // ✅ Интерфейс вместо конкретного UserDbStorage

    public void addEvent(Long userId, Long entityId, String eventType, String operation) {
        eventStorage.addEvent(userId, entityId, eventType, operation);
    }

    public List<Event> getUserFeed(Long userId) {
        userStorage.getUserById(userId); // ✅ Используем интерфейсный метод
        return eventStorage.getUserFeed(userId);
    }
}
