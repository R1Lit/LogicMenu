package pb.r1lit.LogicMenu.gui.service;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.gui.core.MenuActionExecutor;
import pb.r1lit.LogicMenu.gui.model.MenuAction;
import pb.r1lit.LogicMenu.gui.model.MenuRequirement;
import pb.r1lit.LogicMenu.gui.model.MenuRequirementGroup;
import pb.r1lit.LogicMenu.gui.model.MenuState;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MenuRequirementService {

    private final MenuTextResolver resolver;
    private final MenuActionExecutor actionExecutor;
    private final LogicMenu plugin;

    public MenuRequirementService(LogicMenu plugin, MenuTextResolver resolver, MenuActionExecutor actionExecutor) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.actionExecutor = actionExecutor;
    }

    public boolean checkRequirementGroup(Player player, MenuRequirementGroup group, Map<String, String> vars) {
        if (group == null) return true;
        List<MenuRequirement> reqs = group.getRequirements();
        if (reqs == null || reqs.isEmpty()) return true;
        Map<String, String> safeVars = vars == null ? Map.of() : vars;

        int passedOptional = 0;
        for (MenuRequirement req : reqs) {
            boolean ok = checkRequirement(player, req, safeVars);
            if (req.isInverted()) ok = !ok;
            if (ok) {
                if (req.getSuccessActions() != null) {
                    for (MenuAction action : req.getSuccessActions()) {
                        actionExecutor.execute(player, new MenuState("", 0, Map.of()), action, safeVars);
                    }
                }
                if (req.isOptional()) passedOptional++;
                continue;
            }
            if (req.getDenyActions() != null) {
                for (MenuAction action : req.getDenyActions()) {
                    actionExecutor.execute(player, new MenuState("", 0, Map.of()), action, safeVars);
                }
            }
            if (!req.isOptional()) {
                return false;
            }
        }

        int minimum = group.getMinimum();
        if (minimum <= 0) {
            return true;
        }
        return passedOptional >= minimum;
    }

    public void runDenyActions(Player player, MenuRequirementGroup group, Map<String, String> vars) {
        if (group == null) return;
        Map<String, String> safeVars = vars == null ? Map.of() : vars;
        for (MenuAction action : group.getDenyActions()) {
            actionExecutor.execute(player, new MenuState("", 0, Map.of()), action, safeVars);
        }
    }

    public boolean checkRequirement(Player player, MenuRequirement req, Map<String, String> vars) {
        String type = req.getType().toLowerCase(Locale.ROOT);
        Map<String, Object> opt = req.getOptions() == null ? Map.of() : req.getOptions();

        switch (type) {
            case "has permission":
            case "has_permission":
            case "permission": {
                if (opt.get("permissions") instanceof List perms) {
                    for (Object p : perms) {
                        if (!player.hasPermission(String.valueOf(p))) return false;
                    }
                    return true;
                }
                String perm = String.valueOf(opt.getOrDefault("permission", ""));
                return player.hasPermission(perm);
            }
            case "has money":
            case "has_money":
            case "money": {
                String raw = resolver.resolve(String.valueOf(opt.getOrDefault("amount", "0")), player, vars);
                Double parsed = parseDouble(raw);
                double amount = parsed != null ? parsed : 0.0;
                return EconomyBridge.get().has(player, amount);
            }
            case "has item":
            case "has_item": {
                String matName = String.valueOf(opt.getOrDefault("material", ""));
                Double amt = parseDouble(resolver.resolve(String.valueOf(opt.getOrDefault("amount", "1")), player, vars));
                int amount = amt == null ? 1 : (int) Math.max(1, Math.floor(amt));
                String name = String.valueOf(opt.getOrDefault("name", ""));
                Integer data = null;
                Double dataVal = parseDouble(String.valueOf(opt.getOrDefault("data", "")));
                if (dataVal != null) data = dataVal.intValue();
                List<String> lore = opt.containsKey("lore") && opt.get("lore") instanceof List
                        ? (List<String>) opt.get("lore") : List.of();
                return hasItem(player, matName, amount, name, lore, data, vars);
            }
            case "has meta":
            case "has_meta": {
                String key = String.valueOf(opt.getOrDefault("key", ""));
                String typeStr = String.valueOf(opt.getOrDefault("data-type", "string")).toLowerCase(Locale.ROOT);
                String value = String.valueOf(opt.getOrDefault("value", ""));
                if (key.isBlank()) return false;
                NamespacedKey namespacedKey = NamespacedKey.fromString(key, plugin);
                if (namespacedKey == null) return false;

                for (ItemStack item : player.getInventory().getContents()) {
                    if (item == null || !item.hasItemMeta()) continue;
                    var pdc = item.getItemMeta().getPersistentDataContainer();
                    boolean has = switch (typeStr) {
                        case "integer" -> pdc.has(namespacedKey, PersistentDataType.INTEGER)
                                && pdc.get(namespacedKey, PersistentDataType.INTEGER) >= parseIntSafe(value);
                        case "long" -> pdc.has(namespacedKey, PersistentDataType.LONG)
                                && pdc.get(namespacedKey, PersistentDataType.LONG) >= parseLongSafe(value);
                        case "double" -> pdc.has(namespacedKey, PersistentDataType.DOUBLE)
                                && pdc.get(namespacedKey, PersistentDataType.DOUBLE) >= parseDoubleSafe(value);
                        case "string" -> pdc.has(namespacedKey, PersistentDataType.STRING)
                                && String.valueOf(pdc.get(namespacedKey, PersistentDataType.STRING)).equals(value);
                        default -> false;
                    };
                    if (has) return true;
                }
                return false;
            }
            case "javascript":
            case "js": {
                String expression = String.valueOf(opt.getOrDefault("expression", ""));
                return evalJavascript(expression, player, vars);
            }
            case "string contains":
            case "string_contains": {
                String input = String.valueOf(opt.getOrDefault("input", ""));
                String output = String.valueOf(opt.getOrDefault("output", ""));
                String in = resolver.resolve(input, player, vars);
                String out = resolver.resolve(output, player, vars);
                return in.contains(out);
            }
            case "string equals":
            case "string_equals": {
                String input = String.valueOf(opt.getOrDefault("input", ""));
                String output = String.valueOf(opt.getOrDefault("output", ""));
                String in = resolver.resolve(input, player, vars);
                String out = resolver.resolve(output, player, vars);
                return in.equals(out);
            }
            case "string equals ignorecase":
            case "string_equals_ignorecase": {
                String input = String.valueOf(opt.getOrDefault("input", ""));
                String output = String.valueOf(opt.getOrDefault("output", ""));
                String in = resolver.resolve(input, player, vars);
                String out = resolver.resolve(output, player, vars);
                return in.equalsIgnoreCase(out);
            }
            case ">":
            case "<":
            case ">=":
            case "<=":
            case "==":
            case "!=": {
                Double in = parseDouble(resolver.resolve(String.valueOf(opt.getOrDefault("input", "")), player, vars));
                Double out = parseDouble(resolver.resolve(String.valueOf(opt.getOrDefault("output", "")), player, vars));
                if (in == null || out == null) return false;
                return switch (type) {
                    case ">" -> in > out;
                    case "<" -> in < out;
                    case ">=" -> in >= out;
                    case "<=" -> in <= out;
                    case "==" -> in.equals(out);
                    case "!=" -> !in.equals(out);
                    default -> false;
                };
            }
            case "regex matches":
            case "regex_matches": {
                String input = resolver.resolve(String.valueOf(opt.getOrDefault("input", "")), player, vars);
                String pattern = resolver.resolve(String.valueOf(opt.getOrDefault("output", "")), player, vars);
                return input.matches(pattern);
            }
            case "placeholder": {
                String placeholder = String.valueOf(opt.getOrDefault("placeholder", ""));
                String expected = String.valueOf(opt.getOrDefault("equals", ""));
                String actual = resolver.resolve(placeholder, player, vars);
                return actual.equalsIgnoreCase(expected);
            }
            default:
                return false;
        }
    }

    private boolean hasItem(Player player, String matName, int amount, String name, List<String> lore, Integer data, Map<String, String> vars) {
        Material material;
        try {
            material = Material.valueOf(resolver.resolve(matName, player, vars).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            ItemMeta meta = item.getItemMeta();
            if (data != null) {
                if (!(meta instanceof Damageable dmg) || dmg.getDamage() != data) continue;
            }
            if (name != null && !name.isBlank()) {
                String expected = resolver.resolve(name, player, vars);
                if (meta == null || !expected.equals(meta.getDisplayName())) continue;
            }
            if (lore != null && !lore.isEmpty()) {
                List<String> expectedLore = lore.stream().map(l -> resolver.resolve(l, player, vars)).collect(Collectors.toList());
                if (meta == null || meta.getLore() == null || !meta.getLore().equals(expectedLore)) continue;
            }
            count += item.getAmount();
            if (count >= amount) return true;
        }
        return count >= amount;
    }

    private boolean evalJavascript(String expression, Player player, Map<String, String> vars) {
        try {
            javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager().getEngineByName("javascript");
            if (engine == null) engine = new javax.script.ScriptEngineManager().getEngineByName("nashorn");
            if (engine == null) return false;
            engine.put("player", player);
            engine.put("vars", vars);
            Object result = engine.eval(resolver.resolve(expression, player, vars));
            if (result instanceof Boolean) return (Boolean) result;
            return result != null && result.toString().equalsIgnoreCase("true");
        } catch (Exception e) {
            return false;
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

