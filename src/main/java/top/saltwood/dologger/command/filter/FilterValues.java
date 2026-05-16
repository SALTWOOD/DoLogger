package top.saltwood.dologger.command.filter;

import java.util.ArrayList;
import java.util.List;

final class FilterValues {

    private FilterValues() {
    }

    static List<String> commaSeparated(String value, String filterName) throws FilterParseException {
        if (value == null || value.isBlank()) {
            throw new FilterParseException("Missing " + filterName + " filter value");
        }
        String[] parts = value.split(",", -1);
        List<String> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                throw new FilterParseException("Invalid " + filterName + " filter value '" + value + "'; empty entries are not allowed");
            }
            values.add(trimmed);
        }
        return values;
    }

    static List<String> materials(String value, String filterName) throws FilterParseException {
        List<String> materials = commaSeparated(value, filterName);
        List<String> normalized = new ArrayList<>(materials.size());
        for (String material : materials) {
            normalized.add(material.startsWith("minecraft:") ? material.substring("minecraft:".length()) : material);
        }
        return normalized;
    }
}
