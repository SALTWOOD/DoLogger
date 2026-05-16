package top.saltwood.dologger.command.filter;

import java.util.List;

public record UserFilter(List<String> usernames) {

    public UserFilter {
        usernames = List.copyOf(usernames);
    }

    static UserFilter parse(String value) throws FilterParseException {
        return new UserFilter(FilterValues.commaSeparated(value, "user"));
    }

    Object queryValue() {
        return usernames.isEmpty() ? null : usernames.getFirst();
    }
}
