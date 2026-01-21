package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.List;

@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
public class DirectorController {
    private final DirectorService directorService;

    @GetMapping
    public List<Director> getAll() {
        return directorService.findAll();
    }

    @GetMapping("/{id}")
    public Director getById(@PathVariable long id) {
        return directorService.findById(id);
    }

    @PostMapping
    public Director create(@RequestBody Director director) { // Убрали @Valid
        return directorService.save(director);
    }

    @PutMapping
    public Director update(@RequestBody Director director) { // Убрали @Valid
        return directorService.save(director);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        directorService.deleteById(id);
    }
}