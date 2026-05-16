package top.saltwood.dologger.command.filter;

import java.util.List;

public record IncludeFilter(List<String> materials) {

    public IncludeFilter {
        materials = List.copyOf(materials);
    }

    static IncludeFilter parse(String value) throws FilterParseException {
        return new IncludeFilter(FilterValues.materials(value, "include"));
    }

    Object queryValue() {
        return materials.isEmpty() ? null : materials.getFirst();
    }
}
