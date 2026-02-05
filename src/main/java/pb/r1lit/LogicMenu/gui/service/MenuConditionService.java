package pb.r1lit.LogicMenu.gui.service;

import org.bukkit.entity.Player;
import pb.r1lit.LogicMenu.api.LogicMenuApi;
import pb.r1lit.LogicMenu.gui.model.MenuCondition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuConditionService {
    private final MenuTextResolver resolver;
    private LogicMenuApi api;

    public MenuConditionService(MenuTextResolver resolver, LogicMenuApi api) {
        this.resolver = resolver;
        this.api = api;
    }

    public void setApi(LogicMenuApi api) {
        this.api = api;
    }

    public boolean passesConditions(Player player, List<MenuCondition> conditions, Map<String, String> vars, Map<String, String> extraVars) {
        if (conditions == null || conditions.isEmpty()) return true;
        Map<String, String> allVars = new HashMap<>(vars == null ? Map.of() : vars);
        if (extraVars != null) allVars.putAll(extraVars);
        for (MenuCondition condition : conditions) {
            if (!checkCondition(player, condition, allVars)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkCondition(Player player, MenuCondition condition, Map<String, String> vars) {
        return switch (condition.getType()) {
            case PERMISSION -> player.hasPermission(condition.getValue());
            case HAS_TOWN, HAS_NATION, IS_MAYOR -> {
                if (api == null) yield false;
                LogicMenuApi.ConditionHandler handler = api.getConditionHandler(condition.getType().name());
                if (handler == null) yield false;
                try {
                    yield handler.test(player, condition, vars);
                } catch (Throwable t) {
                    if (api instanceof pb.r1lit.LogicMenu.api.LogicMenuApiImpl impl) {
                        impl.unregisterCondition(condition.getType().name());
                    }
                    yield false;
                }
            }
            case VAR_EQUALS -> {
                String[] parts = condition.getValue().split("==", 2);
                if (parts.length != 2) yield false;
                String key = parts[0].trim();
                String expected = parts[1].trim();
                yield expected.equalsIgnoreCase(vars.getOrDefault(key, ""));
            }
            case PLACEHOLDER_EQUALS -> {
                String[] parts = condition.getValue().split("==", 2);
                if (parts.length != 2) yield false;
                String placeholder = parts[0].trim();
                String expected = parts[1].trim();
                String actual = resolver.resolve(placeholder, player, vars);
                yield expected.equalsIgnoreCase(actual);
            }
            case EXPRESSION -> evaluateExpression(condition.getValue(), player, vars);
            default -> {
                if (api == null) yield false;
                LogicMenuApi.ConditionHandler handler = api.getConditionHandler(condition.getType().name());
                if (handler == null) yield false;
                try {
                    yield handler.test(player, condition, vars);
                } catch (Throwable t) {
                    if (api instanceof pb.r1lit.LogicMenu.api.LogicMenuApiImpl impl) {
                        impl.unregisterCondition(condition.getType().name());
                    }
                    yield false;
                }
            }
        };
    }

    private boolean evaluateExpression(String expression, Player player, Map<String, String> vars) {
        if (expression == null || expression.isBlank()) return false;
        String expr = expression.trim();

        String[] ops = new String[] {"==", "!=", ">=", "<=", ">", "<"};
        String op = null;
        int idx = -1;
        for (String candidate : ops) {
            idx = expr.indexOf(candidate);
            if (idx > -1) {
                op = candidate;
                break;
            }
        }
        if (op == null) return false;

        String leftRaw = expr.substring(0, idx).trim();
        String rightRaw = expr.substring(idx + op.length()).trim();

        String left = stripQuotes(resolver.resolve(leftRaw, player, vars));
        String right = stripQuotes(resolver.resolve(rightRaw, player, vars));

        Double leftNum = parseDouble(left);
        Double rightNum = parseDouble(right);

        return switch (op) {
            case "==" -> {
                if (leftNum != null && rightNum != null) yield Double.compare(leftNum, rightNum) == 0;
                yield left.equalsIgnoreCase(right);
            }
            case "!=" -> {
                if (leftNum != null && rightNum != null) yield Double.compare(leftNum, rightNum) != 0;
                yield !left.equalsIgnoreCase(right);
            }
            case ">" -> leftNum != null && rightNum != null && leftNum > rightNum;
            case "<" -> leftNum != null && rightNum != null && leftNum < rightNum;
            case ">=" -> leftNum != null && rightNum != null && leftNum >= rightNum;
            case "<=" -> leftNum != null && rightNum != null && leftNum <= rightNum;
            default -> false;
        };
    }

    private Double parseDouble(String value) {
        if (value == null) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stripQuotes(String value) {
        if (value == null) return "";
        String v = value.trim();
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }
}

