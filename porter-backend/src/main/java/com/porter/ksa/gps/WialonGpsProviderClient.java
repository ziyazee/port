package com.porter.ksa.gps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.porter.ksa.config.WialonGpsProperties;
import com.porter.ksa.entity.GpsDevice;
import com.porter.ksa.entity.GpsProviderType;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class WialonGpsProviderClient implements GpsProviderClient {

    private static final long UNIT_BASIC_AND_LAST_LOCATION_FLAGS = 1025L;

    private final WialonGpsProperties properties;
    private final ObjectMapper objectMapper;
    private final WialonHttpTransport transport;

    @Autowired
    public WialonGpsProviderClient(WialonGpsProperties properties) {
        this(properties, new ObjectMapper(), new JdkWialonHttpTransport(properties));
    }

    WialonGpsProviderClient(
            WialonGpsProperties properties,
            ObjectMapper objectMapper,
            WialonHttpTransport transport
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transport = transport;
    }

    @Override
    public GpsProviderType provider() {
        return GpsProviderType.WIALON;
    }

    @Override
    public List<GpsLocationSnapshot> fetchLatestLocations(List<GpsDevice> devices) {
        List<Long> unitIds = devices.stream()
                .map(GpsDevice::getProviderVehicleId)
                .map(this::normalizeUnitId)
                .distinct()
                .toList();
        if (unitIds.isEmpty()) {
            return List.of();
        }

        String sid = login();
        return unitIds.stream()
                .map(unitId -> fetchCurrentLocationByUnitId(unitId, sid).toGpsLocationSnapshot())
                .collect(Collectors.toList());
    }

    public WialonCurrentLocation fetchCurrentLocationByUnitId(Long unitId) {
        Long normalizedUnitId = normalizeUnitId(unitId);
        return fetchCurrentLocationByUnitId(normalizedUnitId, login());
    }

    private WialonCurrentLocation fetchCurrentLocationByUnitId(Long unitId, String sid) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("id", unitId);
        params.put("flags", UNIT_BASIC_AND_LAST_LOCATION_FLAGS);

        JsonNode response = execute("core/search_item", params, sid);
        JsonNode unit = response.path("item");
        if (!unit.isObject() || unit.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Wialon unit found for unit id");
        }

        JsonNode position = unit.path("pos");
        if (!position.isObject() || position.path("y").isMissingNode() || position.path("x").isMissingNode()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Wialon location found for unit id");
        }

        Instant receivedAt = Instant.now();
        return new WialonCurrentLocation(
                unit.path("id").isNumber() ? unit.path("id").asLong() : unitId,
                textOrNull(unit.path("nm")),
                requiredDouble(position, "y", "latitude"),
                requiredDouble(position, "x", "longitude"),
                position.path("s").isNumber() ? position.path("s").asDouble() : 0.0,
                position.path("c").isNumber() ? position.path("c").asDouble() : 0.0,
                position.path("t").isNumber() ? Instant.ofEpochSecond(position.path("t").asLong()) : receivedAt,
                receivedAt
        );
    }

    private String login() {
        ensureConfigured();

        ObjectNode params = objectMapper.createObjectNode();
        params.put("token", properties.getToken().trim());

        JsonNode response = execute("token/login", params, null);
        String sid = textOrNull(response.path("eid"));
        if (sid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wialon login did not return a session id");
        }

        return sid;
    }

    private JsonNode execute(String service, JsonNode params, String sid) {
        try {
            String paramsJson = objectMapper.writeValueAsString(params);
            WialonHttpResponse response = transport.get(
                    buildUrl(service, paramsJson, sid),
                    Duration.ofMillis(Math.max(1, properties.getReadTimeoutMs()))
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wialon request failed");
            }

            JsonNode body = objectMapper.readTree(response.body());
            if (body.path("error").isNumber()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Wialon request failed with error code " + body.path("error").asInt()
                );
            }

            return body;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wialon request was interrupted", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wialon request failed", exception);
        }
    }

    private String buildUrl(String service, String paramsJson, String sid) {
        StringBuilder url = new StringBuilder(trimTrailingQuestionMark(properties.getBaseUrl()));
        url.append("?svc=").append(encode(service));
        url.append("&params=").append(encode(paramsJson));
        if (sid != null && !sid.isBlank()) {
            url.append("&sid=").append(encode(sid));
        }

        return url.toString();
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

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Wialon GPS provider is disabled");
        }

        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Wialon base URL is not configured");
        }

        if (properties.getToken() == null || properties.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Wialon token is not configured");
        }
    }

    private Long normalizeUnitId(String unitId) {
        String normalized = unitId == null ? null : unitId.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wialon unit id is required");
        }

        if (!normalized.matches("^\\d+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wialon unit id must be a positive number");
        }

        return normalizeUnitId(Long.parseLong(normalized));
    }

    private Long normalizeUnitId(Long unitId) {
        if (unitId == null || unitId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wialon unit id must be a positive number");
        }

        return unitId;
    }

    private double requiredDouble(JsonNode node, String fieldName, String label) {
        JsonNode value = node.path(fieldName);
        if (!value.isNumber()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wialon response is missing " + label);
        }

        return value.asDouble();
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    @FunctionalInterface
    interface WialonHttpTransport {

        WialonHttpResponse get(String url, Duration timeout) throws IOException, InterruptedException;
    }

    record WialonHttpResponse(int statusCode, String body) {
    }

    private static class JdkWialonHttpTransport implements WialonHttpTransport {

        private final HttpClient httpClient;

        JdkWialonHttpTransport(WialonGpsProperties properties) {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.max(1, properties.getConnectTimeoutMs())))
                    .build();
        }

        @Override
        public WialonHttpResponse get(String url, Duration timeout) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new WialonHttpResponse(response.statusCode(), response.body());
        }
    }
}
