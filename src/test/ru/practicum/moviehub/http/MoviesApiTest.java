package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final int PORT = 8080;

    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        gson = new Gson();
        store = new MoviesStore();
        server = new MoviesServer(store, PORT);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode(), "GET /movies должен вернуть 200");
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType,
                "Content-Type должен быть 'application/json; charset=UTF-8'");

        String body = response.body().trim();
        System.out.println(body);
        assertEquals("[]", body, "При пустом списке ответ должен быть '[]'");
    }

    @Test
    void addMovie_andRetrieve() throws Exception {
        String json1 = "{\"title\":\"Фильм 1\",\"year\":2022}";
        HttpRequest addRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json1))
                .build();

        HttpResponse<String> addResponse = client.send(addRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, addResponse.statusCode());

        String getBody = client.send(
                HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ).body();

        assertTrue(getBody.contains("Фильм 1"));

        // Получение по ID
        HttpResponse<String> getByIdResponse = client.send(
                HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/1"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, getByIdResponse.statusCode());
        assertTrue(getByIdResponse.body().contains("Фильм 1"));
    }

    @Test
    void deleteMovie_byId() throws Exception {
        // Добавляем фильм
        String json = "{\"title\":\"Фильм 2\",\"year\":2021}";
        client.send(
                HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        // Удаляем фильм
        HttpResponse<String> deleteResponse = client.send(
                HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/1"))
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(204, deleteResponse.statusCode());

        // Проверяем, что фильма на удаление нет
        HttpResponse<String> getResponse = client.send(
                HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/2")).DELETE().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(404, getResponse.statusCode());
    }

    @Test
    void getMoviesByYear_returnsFiltered() throws Exception {
        // Добавляем фильмы
        String json1 = "{\"title\":\"Фильм 3\",\"year\":2020}";
        String json2 = "{\"title\":\"Фильм 4\",\"year\":2021}";
        String json3 = "{\"title\":\"Фильм 5\",\"year\":2020}";
        client.send(
                HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(json1))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        client.send(
                HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(json2))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        client.send(
                HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(json3))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> responseAll = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        List<Movie> moviesAll = gson.fromJson(responseAll.body(), new ListOfMoviesTypeToken().getType());
        System.out.println(moviesAll.toString());
        // Запрос по году 2020
        HttpResponse<String> response2020 = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies?year=2020"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response2020.statusCode());
        List<Movie> movies = gson.fromJson(response2020.body(), new ListOfMoviesTypeToken().getType());
        System.out.println(movies.toString());
        assertTrue(response2020.body().contains("Фильм 3"));
        assertTrue(response2020.body().contains("Фильм 5"));
        assertFalse(response2020.body().contains("Фильм 4"));
    }

    @Test
    void addMovie_withInvalidData_returnsError() throws Exception {
        String invalidJson = "{\"title\":\"\",\"year\":1800}";
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE + "/movies"))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(402, response.statusCode());
    }

    @Test
    void getNonExistentMovie_returns404() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/9999"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(404, response.statusCode());
    }
}
