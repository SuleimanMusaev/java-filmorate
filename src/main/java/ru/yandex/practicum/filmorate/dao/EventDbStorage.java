package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dao.mappers.EventRowMapper;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Qualifier("eventDbStorage")
public class EventDbStorage implements EventStorage {
    private final JdbcTemplate jdbc;

    private static final String INSERT_EVENT =
            "INSERT INTO events (user_id, entity_id, event_type, operation, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?)";

    private static final String GET_FEED =
            "SELECT * FROM events WHERE user_id = ? ORDER BY timestamp ASC, event_id ASC";

    @Override
    public void addEvent(Long userId, Long entityId, String eventType, String operation) {
        jdbc.update(
                INSERT_EVENT,
                userId,
                entityId,
                eventType,
                operation,
                System.currentTimeMillis()
        );
    }

    @Override
    public List<Event> getUserFeed(Long userId) {
        return jdbc.query(GET_FEED, new EventRowMapper(), userId);
    }
}
