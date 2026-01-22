package ru.yandex.practicum.filmorate.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.service.EventService;

import java.util.List;

@RestController
@AllArgsConstructor
public class EventController {
    private final EventService service;

    @GetMapping("/users/{userId}/feed")
    public List<Event> getFeed(@PathVariable Long userId) {
        return service.getUserFeed(userId);
    }
}
