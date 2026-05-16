package top.saltwood.dologger.command.filter;

import top.saltwood.dologger.model.action.BlockAction;
import top.saltwood.dologger.model.action.IAction;
import top.saltwood.dologger.model.action.ItemAction;
import top.saltwood.dologger.model.action.SessionAction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record ActionFilter(List<Integer> actionIds) {

    private static final Map<String, Integer> ACTION_IDS = createActionIds();

    public ActionFilter {
        actionIds = List.copyOf(actionIds);
    }

    static ActionFilter parse(String value) throws FilterParseException {
        List<String> names = FilterValues.commaSeparated(value, "action");
        List<Integer> ids = new ArrayList<>(names.size());
        for (String name : names) {
            Integer id = ACTION_IDS.get(name.toLowerCase(Locale.ROOT));
            if (id == null) {
                throw new FilterParseException("Invalid action filter value '" + name + "'");
            }
            ids.add(id);
        }
        return new ActionFilter(ids);
    }

    Object queryValue() {
        return actionIds.isEmpty() ? null : actionIds.stream().mapToInt(Integer::intValue).toArray();
    }

    private static Map<String, Integer> createActionIds() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        add(ids, BlockAction.values());
        add(ids, ItemAction.values());
        add(ids, SessionAction.values());
        return Map.copyOf(ids);
    }

    private static void add(Map<String, Integer> ids, IAction[] actions) {
        for (IAction action : actions) {
            ids.put(action.toString().toLowerCase(Locale.ROOT), action.getId());
        }
    }
}
