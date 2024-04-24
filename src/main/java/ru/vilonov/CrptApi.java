package ru.vilonov;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.net.http.HttpRequest.*;
import static java.net.http.HttpRequest.newBuilder;

public class CrptApi {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private final RateLimiter rateLimiter;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        rateLimiter = RateLimiter.create((double) requestLimit/timeUnit.toSeconds(1));
    }

    public synchronized HttpResponse<?> creatingDocumentForProductProducedRUS(Document document, String signature)
            throws IOException, InterruptedException {
        rateLimiter.acquire();
        return httpClient
                .send(newBuilder(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-type", "application/json")
                .POST(BodyPublishers.ofString(createBody(document,signature).toString()))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private Map<String,String> createBody(Document document, String signature) {
        Base64.Encoder encoder = Base64.getEncoder();
        Map<String,String> resultBody = new LinkedHashMap<>();
        resultBody.put("document_format", "MANUAL");
        resultBody.put("product_document", encoder.encodeToString(document.toJson().getBytes()));
        resultBody.put("signature", encoder.encodeToString(signature.getBytes()));
        resultBody.put("type", document.getAttribute("doc_type").toString());
        return resultBody;
    }

    public static class Document {
        private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();
        private final Map<String,Object> attributes;

        private Document(Map<String,Object> attributes) {
            this.attributes = new LinkedHashMap<>(attributes);
        }

        public static Document of(Path path) throws IOException {
            return new Document(GSON.fromJson(Files.readString(path), MAP_TYPE));
        }

        public static Document of(Map<String,Object> attributes){
            return new Document(attributes);
        }

        public Object addAttributes(String key, Object value) {
            attributes.put(key, value);
            return attributes.get(key);
        }

        public Object getAttribute(String key) {
            return attributes.get(key);
        }

        public String toJson() {
            return GSON.toJson(this.attributes);
        }

    }
}


