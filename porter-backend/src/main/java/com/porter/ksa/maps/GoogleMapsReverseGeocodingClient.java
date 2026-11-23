package com.porter.ksa.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.porter.ksa.config.GoogleMapsProperties;
import com.porter.ksa.dto.MapAddressComponentResponse;
import com.porter.ksa.dto.MapDetailsResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoogleMapsReverseGeocodingClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleMapsReverseGeocodingClient.class);
    private static final String PROVIDER = "GOOGLE_GEOCODING";

    private final GoogleMapsProperties properties;
    private final ObjectMapper objectMapper;
    private final MapsHttpTransport transport;

    @Autowired
    public GoogleMapsReverseGeocodingClient(GoogleMapsProperties properties) {
        this(properties, new ObjectMapper(), new JdkMapsHttpTransport(properties));
    }

    GoogleMapsReverseGeocodingClient(
            GoogleMapsProperties properties,
            ObjectMapper objectMapper,
            MapsHttpTransport transport
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transport = transport;
    }

    public Optional<MapDetailsResponse> reverseGeocode(double latitude, double longitude) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            MapsHttpResponse response = transport.get(
                    buildUrl(latitude, longitude),
                    Duration.ofMillis(Math.max(1, properties.getReadTimeoutMs()))
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Google Maps reverse geocode failed with HTTP {}", response.statusCode());
                return Optional.empty();
            }

            JsonNode body = objectMapper.readTree(response.body());
            String status = textOrNull(body.path("status"));
            if (!"OK".equals(status)) {
                log.warn("Google Maps reverse geocode returned status {}", status);
                return Optional.empty();
            }

            JsonNode results = body.path("results");
            if (!results.isArray() || results.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(toMapDetails(results.get(0), body.path("plus_code")));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Google Maps reverse geocode was interrupted", exception);
            return Optional.empty();
        } catch (IOException | IllegalArgumentException exception) {
            log.warn("Google Maps reverse geocode failed", exception);
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        return properties.isEnabled()
                && properties.getBaseUrl() != null
                && !properties.getBaseUrl().isBlank()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank();
    }

    private String buildUrl(double latitude, double longitude) {
        StringBuilder url = new StringBuilder(trimTrailingQuestionMark(properties.getBaseUrl()));
        url.append("?latlng=").append(encode(latitude + "," + longitude));
        url.append("&key=").append(encode(properties.getApiKey().trim()));
        appendOptional(url, "language", properties.getLanguage());
        appendOptional(url, "region", properties.getRegion());
        return url.toString();
    }

    private void appendOptional(StringBuilder url, String key, String value) {
        if (value != null && !value.isBlank()) {
            url.append("&").append(encode(key)).append("=").append(encode(value.trim()));
        }
    }

    private MapDetailsResponse toMapDetails(JsonNode result, JsonNode responsePlusCode) {
        JsonNode plusCode = result.path("plus_code").isObject() ? result.path("plus_code") : responsePlusCode;
        return new MapDetailsResponse(
                PROVIDER,
                textOrNull(result.path("formatted_address")),
                textOrNull(result.path("place_id")),
                textOrNull(plusCode.path("global_code")),
                textOrNull(plusCode.path("compound_code")),
                textList(result.path("types")),
                addressComponents(result.path("address_components"))
        );
    }

    private List<MapAddressComponentResponse> addressComponents(JsonNode components) {
        if (!components.isArray()) {
            return List.of();
        }

        List<MapAddressComponentResponse> responses = new ArrayList<>();
        for (JsonNode component : components) {
            responses.add(new MapAddressComponentResponse(
                    textOrNull(component.path("long_name")),
                    textOrNull(component.path("short_name")),
                    textList(component.path("types"))
            ));
        }

        return responses;
    }

    private List<String> textList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = textOrNull(item);
            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String trimTrailingQuestionMark(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("?")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    interface MapsHttpTransport {

        MapsHttpResponse get(String url, Duration timeout) throws IOException, InterruptedException;
    }

    record MapsHttpResponse(int statusCode, String body) {
    }

    private static class JdkMapsHttpTransport implements MapsHttpTransport {

        private final HttpClient httpClient;

        JdkMapsHttpTransport(GoogleMapsProperties properties) {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.max(1, properties.getConnectTimeoutMs())))
                    .build();
        }

        @Override
        public MapsHttpResponse get(String url, Duration timeout) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new MapsHttpResponse(response.statusCode(), response.body());
        }
    }
}
