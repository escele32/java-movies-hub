package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    // !!! Укажите содержимое заголовка Content-Type
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final Gson gson = new Gson();

    protected void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        // !!! Реализуйте общий для всех хендлеров метод
        // для отправки ответа с телом в формате JSON
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        // !!! Реализуйте общий для всех хендлеров метод
        // для отправки ответа без тела и кодом 204
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    protected void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String response = gson.toJson(new ErrorResponse(message));
        exchange.getResponseHeaders().set("Content-type", CT_JSON);
        exchange.sendResponseHeaders(status, response.getBytes().length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response.getBytes());
        }
    }
}