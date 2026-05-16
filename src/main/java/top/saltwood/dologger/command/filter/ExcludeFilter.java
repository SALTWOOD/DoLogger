package top.saltwood.dologger.command.filter;

import java.util.List;

public record ExcludeFilter(List<String> materials) {

    public ExcludeFilter {
        materials = List.copyOf(materials);
    }

    static ExcludeFilter parse(String value) throws FilterParseException {
        return new ExcludeFilter(FilterValues.materials(value, "exclude"));
    }

    Object queryValue() {
        return materials.isEmpty() ? null : materials.getFirst();
    }
}
