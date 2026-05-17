package top.saltwood.dologger.command.filter;

import top.saltwood.dologger.model.action.BlockAction;
import top.saltwood.dologger.model.action.CommandAction;
import top.saltwood.dologger.model.action.ChatAction;
import top.saltwood.dologger.model.action.IAction;
import top.saltwood.dologger.model.action.ItemAction;
import top.saltwood.dologger.model.action.SessionAction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record ActionFilter(Map<ActionFamily, List<Integer>> actionIds, Set<ActionFamily> selectedFamilies) {

    private static final Map<String, ActionToken> ACTIONS = createActions();

    public ActionFilter {
        actionIds = copyActionIds(actionIds);
        selectedFamilies = Set.copyOf(selectedFamilies);
    }

    static ActionFilter parse(String value) throws FilterParseException {
        List<String> names = FilterValues.commaSeparated(value, "action");
        Map<ActionFamily, List<Integer>> ids = new EnumMap<>(ActionFamily.class);
        Set<ActionFamily> families = EnumSet.noneOf(ActionFamily.class);
        for (String name : names) {
            ActionToken token = ACTIONS.get(name.toLowerCase(Locale.ROOT));
            if (token == null) {
                throw new FilterParseException("Invalid action filter value '" + name + "'");
            }
            families.add(token.family());
            if (token.id() != null) {
                ids.computeIfAbsent(token.family(), ignored -> new ArrayList<>()).add(token.id());
            }
        }
        return new ActionFilter(ids, families);
    }

    public boolean selects(ActionFamily family) {
        return selectedFamilies.contains(family);
    }

    public Object queryValue(ActionFamily family) {
        List<Integer> ids = actionIds.get(family);
        return ids == null || ids.isEmpty() ? null : ids.stream().mapToInt(Integer::intValue).toArray();
    }

    private static Map<String, ActionToken> createActions() {
        Map<String, ActionToken> actions = new LinkedHashMap<>();
        add(actions, ActionFamily.BLOCK, BlockAction.values());
        add(actions, ActionFamily.ITEM, ItemAction.values());
        add(actions, ActionFamily.SESSION, SessionAction.values());
        add(actions, ActionFamily.CHAT, ChatAction.values());
        add(actions, ActionFamily.COMMAND, CommandAction.values());
        return Map.copyOf(actions);
    }

    private static void add(Map<String, ActionToken> tokens, ActionFamily family, IAction[] actions) {
        for (IAction action : actions) {
            tokens.put(action.toString().toLowerCase(Locale.ROOT), new ActionToken(family, action.getId()));
        }
    }

    private static Map<ActionFamily, List<Integer>> copyActionIds(Map<ActionFamily, List<Integer>> actionIds) {
        Map<ActionFamily, List<Integer>> copy = new EnumMap<>(ActionFamily.class);
        for (Map.Entry<ActionFamily, List<Integer>> entry : actionIds.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    private record ActionToken(ActionFamily family, Integer id) {
    }
}
