package pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

final class ProviderJson {
    private ProviderJson() {}

    static String text(JsonNode n, String key) {
        var v = n.get(key);
        if (v == null || v.isNull()) return null;
        if (!v.isTextual()) throw new IllegalArgumentException("Invalid " + key);
        return v.asText().isBlank() ? null : v.asText().strip();
    }

    static List<String> strings(JsonNode n, String key, String field) {
        var v = n.get(key);
        if (v == null || v.isNull()) return List.of();
        if (!v.isArray()) throw new IllegalArgumentException("Invalid " + key);
        List<String> values = new ArrayList<>();
        for (var item : v) {
            String value;
            if (field == null) {
                if (!item.isTextual()) throw new IllegalArgumentException("Invalid " + key);
                value = item.asText();
            } else {
                if (!item.isObject()) throw new IllegalArgumentException("Invalid " + key);
                value = text(item, field);
            }
            if (value != null && !value.isBlank()) values.add(value.strip());
        }
        return values;
    }
}
