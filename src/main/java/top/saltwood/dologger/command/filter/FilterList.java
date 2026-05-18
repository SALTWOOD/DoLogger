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
    private final @Nullable LimitFilter limit;

    FilterList(@Nullable UserFilter user, @Nullable TimeFilter time, @Nullable RadiusFilter radius, @Nullable ActionFilter action, @Nullable IncludeFilter include, @Nullable ExcludeFilter exclude, @Nullable LimitFilter limit) {
        this.user = user;
        this.time = time;
        this.radius = radius;
        this.action = action;
        this.include = include;
        this.exclude = exclude;
        this.limit = limit;
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

    public @Nullable LimitFilter limit() {
        return limit;
    }

    public List<Object> toRepositoryParams() {
        return toBlockRepositoryParams();
    }

    public List<Object> toBlockRepositoryParams() {
        return toRepositoryParams(ActionFamily.BLOCK, true);
    }

    public List<Object> toItemRepositoryParams() {
        return toRepositoryParams(ActionFamily.ITEM, true);
    }

    public List<Object> toSessionRepositoryParams() {
        return toRepositoryParams(ActionFamily.SESSION, false);
    }

    public List<Object> toChatRepositoryParams() {
        return toRepositoryParams(ActionFamily.CHAT, false);
    }

    public List<Object> toCommandRepositoryParams() {
        return toRepositoryParams(ActionFamily.COMMAND, false);
    }

    public boolean selects(ActionFamily family) {
        return action == null || action.selects(family);
    }

    private List<Object> toRepositoryParams(ActionFamily family, boolean includeMaterialFilters) {
        List<Object> params = new ArrayList<>(13);
        params.add(user == null ? null : user.queryValue());
        params.add(time == null ? null : time.afterCutoffMillis());
        params.add(time == null ? null : time.beforeCutoffMillis());
        params.add(radius == null ? null : radius.queryMinXValue());
        params.add(radius == null ? null : radius.queryMaxXValue());
        params.add(radius == null ? null : radius.queryMinYValue());
        params.add(radius == null ? null : radius.queryMaxYValue());
        params.add(radius == null ? null : radius.queryMinZValue());
        params.add(radius == null ? null : radius.queryMaxZValue());
        params.add(action == null ? null : action.queryValue(family));
        if (includeMaterialFilters) {
            params.add(include == null ? null : include.queryValue());
            params.add(exclude == null ? null : exclude.queryValue());
        }
        params.add(limit == null ? null : limit.queryValue());
        return params;
    }
}
