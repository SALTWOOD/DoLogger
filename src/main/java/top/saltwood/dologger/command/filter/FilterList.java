package top.saltwood.dologger.command.filter;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FilterList {

    private final @Nullable UserFilter user;
    private final @Nullable TimeFilter time;
    private final @Nullable RadiusFilter radius;
    private final @Nullable ActionFilter action;
    private final @Nullable IncludeFilter include;
    private final @Nullable ExcludeFilter exclude;

    FilterList(@Nullable UserFilter user, @Nullable TimeFilter time, @Nullable RadiusFilter radius, @Nullable ActionFilter action, @Nullable IncludeFilter include, @Nullable ExcludeFilter exclude) {
        this.user = user;
        this.time = time;
        this.radius = radius;
        this.action = action;
        this.include = include;
        this.exclude = exclude;
    }

    public @Nullable UserFilter user() {
        return user;
    }

    public @Nullable TimeFilter time() {
        return time;
    }

    public @Nullable RadiusFilter radius() {
        return radius;
    }

    public @Nullable ActionFilter action() {
        return action;
    }

    public @Nullable IncludeFilter include() {
        return include;
    }

    public @Nullable ExcludeFilter exclude() {
        return exclude;
    }

    public List<Object> toRepositoryParams() {
        List<Object> params = new ArrayList<>(8);
        params.add(user == null ? null : user.queryValue());
        params.add(time == null ? null : time.queryValue());
        params.add(null);
        params.add(radius == null ? null : radius.queryXValue());
        params.add(radius == null ? null : radius.queryYValue());
        params.add(radius == null ? null : radius.queryZValue());
        params.add(action == null ? null : action.queryValue());
        params.add(include == null ? null : include.queryValue());
        return params;
    }

    public List<Object> toSessionRepositoryParams() {
        return toRepositoryParams().subList(0, 7);
    }
}
