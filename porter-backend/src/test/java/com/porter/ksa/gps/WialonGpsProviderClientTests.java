package com.porter.ksa.gps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.porter.ksa.config.WialonGpsProperties;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class WialonGpsProviderClientTests {

    private final List<Map<String, String>> requests = new ArrayList<>();

    @Test
    void fetchCurrentLocationByUnitIdLogsInSearchesByIdAndMapsPosition() {
        WialonGpsProviderClient client = new WialonGpsProviderClient(
                properties(),
                new ObjectMapper(),
                this::respond
        );

        WialonCurrentLocation location = client.fetchCurrentLocationByUnitId(34868L);

        assertEquals(34868L, location.wialonUnitId());
        assertEquals("Truck 101", location.unitName());
        assertEquals(24.7136, location.latitude());
        assertEquals(46.6753, location.longitude());
        assertEquals(42.0, location.speedKph());
        assertEquals(180.0, location.headingDegrees());
        assertEquals(Instant.ofEpochSecond(1785156720L), location.recordedAt());

        assertEquals("token/login", requests.get(0).get("svc"));
        assertEquals("core/search_item", requests.get(1).get("svc"));
        assertEquals("sid-1", requests.get(1).get("sid"));
        assertTrue(requests.get(1).get("params").contains("\"id\":34868"));
        assertTrue(requests.get(1).get("params").contains("\"flags\":1025"));
    }

    @Test
    void fetchCurrentLocationByUnitIdRejectsInvalidUnitIdBeforeRemoteCall() {
        WialonGpsProviderClient client = new WialonGpsProviderClient(
                properties(),
                new ObjectMapper(),
                this::respond
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> client.fetchCurrentLocationByUnitId(0L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(requests.isEmpty());
    }

    private WialonGpsProviderClient.WialonHttpResponse respond(String url, java.time.Duration timeout) {
        Map<String, String> query = parseQuery(URI.create(url).getRawQuery());
        requests.add(query);

        if ("token/login".equals(query.get("svc"))) {
            return new WialonGpsProviderClient.WialonHttpResponse(200, "{\"eid\":\"sid-1\"}");
        }

        if ("core/search_item".equals(query.get("svc"))) {
            return new WialonGpsProviderClient.WialonHttpResponse(200, """
                    {
                      "item": {
                          "id": 34868,
                          "nm": "Truck 101",
                          "pos": {
                            "t": 1785156720,
                            "y": 24.7136,
                            "x": 46.6753,
                            "s": 42,
                            "c": 180
                          }
                        }
                    }
                    """);
        }

        return new WialonGpsProviderClient.WialonHttpResponse(200, "{\"error\":4}");
    }

    private WialonGpsProperties properties() {
        WialonGpsProperties properties = new WialonGpsProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://wialon.test/wialon/ajax.html");
        properties.setToken("test-token");
        properties.setConnectTimeoutMs(1000);
        properties.setReadTimeoutMs(1000);
        return properties;
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }

        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            query.put(key, value);
        }

        return query;
    }
}
