package ru.practicum.moviehub.http;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private MoviesStore store;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        switch (method.toUpperCase()) {
            case "GET":
                if (path.equals("/movies")) {
                    if (query == null) {
                        handleGetAllMovie(exchange);
                    } else if (query.contains("year=")) {
                        handleGetYearMovie(exchange);
                    }
                } else if (path.startsWith("/movies/")) {
                    handleGetIdMovie(exchange);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                }
                break;
            case "POST":
                if (path.equals("/movies")) {
                    handlePostMovie(exchange);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                }
                break;
            case "DELETE":
                if (path.startsWith("/movies/")) {
                    handleDeleteIdMovie(exchange);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                }
                break;
            default:
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
        }
    }

    private void handleGetAllMovie(HttpExchange exchange) throws IOException {
        List<Movie> movies = store.getAll();
        String jsonResponse = gson.toJson(movies);
        sendJson(exchange, 200, jsonResponse);
    }

    private void handleGetIdMovie(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length != 3) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }
        Long id;
        try {
            id = Long.parseLong(parts[2]);
        } catch (NumberFormatException exception) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }
        Optional<Movie> movie = store.getById(id);
        if (movie.isEmpty()) {
            sendError(exchange, 404, "Фильм не найден");
        } else {
            String jsonResponse = gson.toJson(movie.get());
            sendJson(exchange, 200, jsonResponse);
        }
    }

    private void handleGetYearMovie(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int year = 0;
        if (query != null && query.contains("year=")) {
            try {
                year = Integer.parseInt(query.substring(5));
            } catch (NumberFormatException exception) {
                sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
                return;
            }
            if (year < 1888 || year > Year.now().getValue()) {
                sendError(exchange, 400, "Год должен быть между 1888 и " + (Year.now().getValue()));
                return;
            }
            List<Movie> movies = store.getByYear(year);
            String jsonResponse = gson.toJson(movies);
            sendJson(exchange, 200, jsonResponse);
        } else {
            sendError(exchange, 400, "Отсутствует параметр 'year'");
        }
    }

    private void handlePostMovie(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/json")) {
            exchange.sendResponseHeaders(415, -1);
            return;
        }

        JsonObject requestBody;
        try (InputStreamReader inputStreamReader =
                     new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestBody = gson.fromJson(inputStreamReader, JsonObject.class);
        } catch (Exception exception) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        List<String> errors = new ArrayList<>();
        String title = null;
        if (requestBody.has("title")) {
            title = requestBody.get("title").getAsString().trim();
            if (title.isEmpty()) {
                errors.add("Название не должно быть пустым");
            } else if (title.length() > 100) {
                errors.add("Название не должно превышать 100 символов");
            }
        } else {
            errors.add("Отсутствует поле 'title'");
        }
        int year = 0;
        if (requestBody.has("year")) {
            try {
                year = requestBody.get("year").getAsInt();
            } catch (Exception exception) {
                errors.add("Год должен быть числом");
            }
            if (year < 1888 || year > Year.now().getValue() + 1) {
                errors.add("Год должен быть между 1888 и " + (Year.now().getValue() + 1));
            }
        } else {
            errors.add("Отсутствует поле 'year'");
        }
        if (!errors.isEmpty()) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Ошибка валидации");
            errorResponse.add("details", gson.toJsonTree(errors));
            String responseJson = gson.toJson(errorResponse);
            sendJson(exchange, 402, responseJson);
            return;
        }

        Movie newMovie = store.addMovieInMap(title, year);
        String responseJson = gson.toJson(newMovie);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(201,responseJson.getBytes().length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseJson.getBytes());
        }
    }

    private void handleDeleteIdMovie(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length != 3) {
            sendError(exchange, 400, "Некорректный запрос");
            return;
        }
        try {
            Long id = Long.parseLong(parts[2]);
            Movie movie = store.delete(id);
            if(movie == null) {
                sendNoContent(exchange);
            } else {
                sendError(exchange, 404, "Фильм не найден");
            }
        } catch (Exception exception) {
            sendError(exchange, 400, "Некорректный ID");
        }
    }
}
