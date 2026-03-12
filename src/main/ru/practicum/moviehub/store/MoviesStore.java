package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MoviesStore {
    private Map<Long, Movie> mapFilms = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Movie addMovieInMap(String title, int year) {
        Long id = idGenerator.getAndIncrement();
        Movie movie = new Movie(title, year);
        movie.setId(id);
        mapFilms.put(id, movie);
        return movie;
    }

    public void clear() {
        mapFilms.clear();
    }

    public Optional<Movie> getById(Long id) {
        return Optional.ofNullable(mapFilms.get(id));
    }

    public Movie delete(Long id) {
        return mapFilms.remove(id);
    }

    public List<Movie> getAll() {
        return new ArrayList<>(mapFilms.values());
    }

    public List<Movie> getByYear(int year) {
        return mapFilms.values().stream()
                .filter(film -> film.getYear() == year)
                .collect(Collectors.toList());
    }
}