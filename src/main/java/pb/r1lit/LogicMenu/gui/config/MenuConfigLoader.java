package pb.r1lit.LogicMenu.gui.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import pb.r1lit.LogicMenu.gui.model.MenuAction;
import pb.r1lit.LogicMenu.gui.model.MenuCondition;
import pb.r1lit.LogicMenu.gui.model.MenuDefinition;
import pb.r1lit.LogicMenu.gui.model.MenuDynamicDefinition;
import pb.r1lit.LogicMenu.gui.model.MenuFillDefinition;
import pb.r1lit.LogicMenu.gui.model.MenuItemDefinition;
import pb.r1lit.LogicMenu.gui.model.MenuRequirement;
import pb.r1lit.LogicMenu.gui.model.MenuRequirementGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuConfigLoader {

    public Map<String, MenuDefinition> loadSingle(FileConfiguration config, String id) {
        Map<String, MenuDefinition> result = new HashMap<>();
        MenuDefinition def = parseMenu(id, config);
        if (def != null) {
            result.put(id, def);
        }
        return result;
    }

    public Map<String, MenuDefinition> load(FileConfiguration config) {
        Map<String, MenuDefinition> result = new HashMap<>();
        ConfigurationSection menus = config.getConfigurationSection("gui.menus");
        if (menus == null) return result;

        for (String id : menus.getKeys(false)) {
            ConfigurationSection menuSec = menus.getConfigurationSection(id);
            if (menuSec == null) continue;
            MenuDefinition def = parseMenu(id, menuSec);
            if (def != null) {
                result.put(id, def);
            }
        }

        return result;
    }

    private MenuDefinition parseMenu(String id, ConfigurationSection menuSec) {
        String title = menuSec.getString("title", id);
        int size = menuSec.getInt("size", 9);
        String permission = menuSec.getString("permission", "");
        boolean update = menuSec.getBoolean("update", false);
        int updateIntervalTicks = menuSec.getInt("update-interval", 20) * 20;

        Map<String, String> vars = new HashMap<>();
        ConfigurationSection varsSec = menuSec.getConfigurationSection("vars");
        if (varsSec != null) {
            for (String key : varsSec.getKeys(false)) {
                vars.put(key, varsSec.getString(key, ""));
            }
        }

        List<MenuItemDefinition> items = new ArrayList<>();
        ConfigurationSection itemsSec = menuSec.getConfigurationSection("items");
        if (itemsSec != null) {
            for (String key : itemsSec.getKeys(false)) {
                ConfigurationSection itemSec = itemsSec.getConfigurationSection(key);
                if (itemSec == null) continue;
                items.addAll(parseItemVariants(itemSec));
            }
        }

        MenuItemDefinition fillItem = null;
        List<Integer> fillSlots = new ArrayList<>();
        String fillSlotRange = "";
        String fillType = "";
        List<MenuFillDefinition> fills = new ArrayList<>();
        ConfigurationSection fillSec = menuSec.getConfigurationSection("fill");
        if (fillSec != null) {
            String material = fillSec.getString("material", "BLACK_STAINED_GLASS_PANE");
            String name = fillSec.getString("name", " ");
            List<String> lore = fillSec.getStringList("lore");
            fillItem = MenuItemDefinition.emptyFill(material, name, lore);
            fillSlots.addAll(fillSec.getIntegerList("slots"));
            fillSlotRange = fillSec.getString("slot-range", "");
            fillType = fillSec.getString("type", "");
            int priority = fillSec.getInt("priority", 100);
            fills.add(new MenuFillDefinition(fillItem, new ArrayList<>(fillSlots), fillSlotRange, fillType, priority));
        }
        if (menuSec.isList("fills")) {
            for (Object raw : menuSec.getList("fills")) {
                if (!(raw instanceof java.util.Map<?, ?> map)) continue;
                MenuFillDefinition def = parseFillMap(map);
                if (def != null) fills.add(def);
            }
        }

        MenuDynamicDefinition dynamic = null;
        ConfigurationSection dynSec = menuSec.getConfigurationSection("dynamic");
        if (dynSec != null) {
            String provider = dynSec.getString("provider", "");
            int startSlot = dynSec.getInt("start-slot", 0);
            int endSlot = dynSec.getInt("end-slot", size - 1);
            int pageSize = dynSec.getInt("page-size", (endSlot - startSlot) + 1);

            MenuItemDefinition template = null;
            ConfigurationSection itemSec = dynSec.getConfigurationSection("item");
            if (itemSec != null) {
                template = parseItem(itemSec);
            }

            dynamic = new MenuDynamicDefinition(provider, startSlot, endSlot, pageSize, template);
        }

        List<MenuDynamicDefinition> dynamics = new ArrayList<>();
        ConfigurationSection dynamicsSec = menuSec.getConfigurationSection("dynamics");
        if (dynamicsSec != null) {
            for (String key : dynamicsSec.getKeys(false)) {
                ConfigurationSection dsec = dynamicsSec.getConfigurationSection(key);
                if (dsec == null) continue;
                String provider = dsec.getString("provider", "");
                int startSlot = dsec.getInt("start-slot", 0);
                int endSlot = dsec.getInt("end-slot", size - 1);
                int pageSize = dsec.getInt("page-size", (endSlot - startSlot) + 1);

                MenuItemDefinition template = null;
                ConfigurationSection itemSec = dsec.getConfigurationSection("item");
                if (itemSec != null) {
                    template = parseItem(itemSec);
                }

                dynamics.add(new MenuDynamicDefinition(provider, startSlot, endSlot, pageSize, template));
            }
        }

        MenuRequirementGroup openRequirement = parseRequirementGroup(menuSec.getConfigurationSection("open-requirement"));
        if (openRequirement == null) {
            openRequirement = parseRequirementGroup(menuSec.getConfigurationSection("open_requirement"));
        }
        if (openRequirement == null) {
            String expr = menuSec.getString("open-requirement", "");
            if (!expr.isBlank()) {
                openRequirement = simpleJavascriptRequirement(expr);
            }
        }

        List<String> openCommands = new ArrayList<>();
        if (menuSec.isList("open-command")) {
            openCommands.addAll(menuSec.getStringList("open-command"));
        } else if (menuSec.isList("open_command")) {
            openCommands.addAll(menuSec.getStringList("open_command"));
        } else {
            String cmd = menuSec.getString("open-command", "");
            if (cmd.isBlank()) {
                cmd = menuSec.getString("open_command", "");
            }
            if (!cmd.isBlank()) openCommands.add(cmd);
        }

        return new MenuDefinition(id, title, size, items, fillItem, dynamic, dynamics, vars, permission,
                update, updateIntervalTicks, openRequirement, fillSlots, fillSlotRange, fillType, fills, openCommands);
    }

    private MenuFillDefinition parseFillMap(java.util.Map<?, ?> map) {
        if (map == null) return null;
        String material = asString(map.get("material"), "BLACK_STAINED_GLASS_PANE");
        String name = asString(map.get("name"), " ");
        List<String> lore = asStringList(map.get("lore"));
        MenuItemDefinition item = MenuItemDefinition.emptyFill(material, name, lore);

        List<Integer> slots = new ArrayList<>();
        Object slotsRaw = map.get("slots");
        if (slotsRaw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Number n) slots.add(n.intValue());
                else if (o instanceof String s) {
                    try { slots.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
                }
            }
        }
        String slotRange = asString(map.get("slot-range"), "");
        String type = asString(map.get("type"), "");
        int priority = asInt(map.get("priority"), 100);

        return new MenuFillDefinition(item, slots, slotRange, type, priority);
    }

    private String asString(Object value, String def) {
        return value == null ? def : String.valueOf(value);
    }

    private int asInt(Object value, int def) {
        if (value == null) return def;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException e) { return def; }
    }

    private List<String> asStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) out.add(String.valueOf(o));
            }
        }
        return out;
    }

    private MenuItemDefinition parseItem(ConfigurationSection sec) {
        int slot = sec.getInt("slot", -1);
        List<Integer> slots = sec.getIntegerList("slots");
        String slotRange = sec.getString("slot-range", "");
        if (slotRange != null && !slotRange.isBlank()) {
            String[] parts = slotRange.split("-");
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());
                    for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                        slots.add(i);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String material = sec.getString("material", "STONE");
        String name = sec.getString("name", "");
        List<String> lore = sec.getStringList("lore");
        int amount = sec.getInt("amount", 1);
        boolean glow = sec.getBoolean("glow", false);
        Integer modelData = sec.contains("model-data") ? sec.getInt("model-data") : null;
        boolean hideAttributes = sec.getBoolean("hide-attributes", true);
        int priority = sec.getInt("priority", 0);
        boolean update = sec.getBoolean("update", false);
        String permission = sec.getString("permission", "");
        boolean unbreakable = sec.getBoolean("unbreakable", false);
        List<String> itemFlags = sec.getStringList("item-flags");
        List<String> enchantments = sec.getStringList("enchantments");

        List<MenuAction> actions = new ArrayList<>();
        List<String> rawActions = sec.getStringList("actions");
        for (String raw : rawActions) {
            actions.add(MenuAction.parse(raw));
        }

        Map<String, List<MenuAction>> clickActions = new HashMap<>();
        ConfigurationSection clickSec = sec.getConfigurationSection("click-actions");
        if (clickSec != null) {
            for (String key : clickSec.getKeys(false)) {
                List<MenuAction> list = new ArrayList<>();
                for (String raw : clickSec.getStringList(key)) {
                    list.add(MenuAction.parse(raw));
                }
                clickActions.put(key.toLowerCase(), list);
            }
        }

        List<MenuCondition> conditions = new ArrayList<>();
        for (String raw : sec.getStringList("conditions")) {
            MenuCondition condition = MenuCondition.parse(raw);
            if (condition != null) conditions.add(condition);
        }

        MenuRequirementGroup viewRequirement = parseRequirementGroup(sec.getConfigurationSection("view-requirement"));
        if (viewRequirement == null) {
            viewRequirement = parseRequirementGroup(sec.getConfigurationSection("view_requirement"));
        }
        if (viewRequirement == null) {
            viewRequirement = parseRequirementGroup(sec.getConfigurationSection("requirements"));
        }
        if (viewRequirement == null) {
            String expr = sec.getString("view-requirement", "");
            if (!expr.isBlank()) {
                viewRequirement = simpleJavascriptRequirement(expr);
            }
        }
        MenuRequirementGroup leftClickRequirement = parseRequirementGroup(sec.getConfigurationSection("left-click-requirement"));
        if (leftClickRequirement == null) {
            leftClickRequirement = parseRequirementGroup(sec.getConfigurationSection("left_click_requirement"));
        }
        if (leftClickRequirement == null) {
            String expr = sec.getString("left-click-requirement", "");
            if (!expr.isBlank()) {
                leftClickRequirement = simpleJavascriptRequirement(expr);
            }
        }
        MenuRequirementGroup rightClickRequirement = parseRequirementGroup(sec.getConfigurationSection("right-click-requirement"));
        if (rightClickRequirement == null) {
            rightClickRequirement = parseRequirementGroup(sec.getConfigurationSection("right_click_requirement"));
        }
        if (rightClickRequirement == null) {
            String expr = sec.getString("right-click-requirement", "");
            if (!expr.isBlank()) {
                rightClickRequirement = simpleJavascriptRequirement(expr);
            }
        }

        return new MenuItemDefinition(slot, slots, material, name, lore, actions, conditions,
                clickActions, amount, glow, modelData, hideAttributes, priority, update, permission,
                viewRequirement, leftClickRequirement, rightClickRequirement,
                itemFlags, enchantments, unbreakable);
    }

    private List<MenuItemDefinition> parseItemVariants(ConfigurationSection sec) {
        MenuItemDefinition base = parseItem(sec);
        ConfigurationSection variantsSec = sec.getConfigurationSection("variants");
        if (variantsSec == null) {
            return List.of(base);
        }

        List<MenuItemDefinition> result = new ArrayList<>();
        boolean hasElse = false;

        for (String key : variantsSec.getKeys(false)) {
            ConfigurationSection v = variantsSec.getConfigurationSection(key);
            if (v == null) continue;

            MenuItemDefinition def = applyVariant(base, v);
            result.add(def);

            boolean isElse = v.getBoolean("else", false);
            String ifExpr = v.getString("if", "");
            if (isElse || ifExpr == null || ifExpr.isBlank()) {
                hasElse = true;
            }
        }

        if (!hasElse) {
            result.add(base);
        }

        return result;
    }

    private MenuItemDefinition applyVariant(MenuItemDefinition base, ConfigurationSection v) {
        String material = v.contains("material") ? v.getString("material", base.getMaterial()) : base.getMaterial();
        String name = v.contains("name") ? v.getString("name", base.getName()) : base.getName();
        List<String> lore = v.isList("lore") ? v.getStringList("lore") : base.getLore();
        int amount = v.contains("amount") ? v.getInt("amount", base.getAmount()) : base.getAmount();
        boolean glow = v.contains("glow") ? v.getBoolean("glow", base.isGlow()) : base.isGlow();
        Integer modelData = v.contains("model-data") ? Integer.valueOf(v.getInt("model-data")) : base.getModelData();
        boolean hideAttributes = v.contains("hide-attributes") ? v.getBoolean("hide-attributes", base.isHideAttributes()) : base.isHideAttributes();
        int priority = v.contains("priority") ? v.getInt("priority", base.getPriority()) : base.getPriority();
        boolean update = v.contains("update") ? v.getBoolean("update", base.isUpdate()) : base.isUpdate();
        String permission = v.contains("permission") ? v.getString("permission", base.getPermission()) : base.getPermission();
        boolean unbreakable = v.contains("unbreakable") ? v.getBoolean("unbreakable", base.isUnbreakable()) : base.isUnbreakable();
        List<String> itemFlags = v.isList("item-flags") ? v.getStringList("item-flags") : base.getItemFlags();
        List<String> enchantments = v.isList("enchantments") ? v.getStringList("enchantments") : base.getEnchantments();

        List<MenuCondition> conditions = new ArrayList<>();
        if (base.getConditions() != null) conditions.addAll(base.getConditions());

        String ifExpr = v.getString("if", "");
        if (ifExpr != null && !ifExpr.isBlank()) {
            MenuCondition condition = MenuCondition.parse(ifExpr);
            if (condition != null) conditions.add(condition);
        }
        if (v.isList("conditions")) {
            for (String raw : v.getStringList("conditions")) {
                MenuCondition condition = MenuCondition.parse(raw);
                if (condition != null) conditions.add(condition);
            }
        }

        MenuRequirementGroup viewRequirement = base.getViewRequirement();
        MenuRequirementGroup leftClickRequirement = base.getLeftClickRequirement();
        MenuRequirementGroup rightClickRequirement = base.getRightClickRequirement();

        if (v.getConfigurationSection("view-requirement") != null || v.getConfigurationSection("view_requirement") != null
                || v.getConfigurationSection("requirements") != null || v.isString("view-requirement")) {
            MenuRequirementGroup vr = parseRequirementGroup(v.getConfigurationSection("view-requirement"));
            if (vr == null) vr = parseRequirementGroup(v.getConfigurationSection("view_requirement"));
            if (vr == null) vr = parseRequirementGroup(v.getConfigurationSection("requirements"));
            if (vr == null) {
                String expr = v.getString("view-requirement", "");
                if (expr != null && !expr.isBlank()) vr = simpleJavascriptRequirement(expr);
            }
            if (vr != null) viewRequirement = vr;
        }

        if (v.getConfigurationSection("left-click-requirement") != null || v.getConfigurationSection("left_click_requirement") != null
                || v.isString("left-click-requirement")) {
            MenuRequirementGroup lr = parseRequirementGroup(v.getConfigurationSection("left-click-requirement"));
            if (lr == null) lr = parseRequirementGroup(v.getConfigurationSection("left_click_requirement"));
            if (lr == null) {
                String expr = v.getString("left-click-requirement", "");
                if (expr != null && !expr.isBlank()) lr = simpleJavascriptRequirement(expr);
            }
            if (lr != null) leftClickRequirement = lr;
        }

        if (v.getConfigurationSection("right-click-requirement") != null || v.getConfigurationSection("right_click_requirement") != null
                || v.isString("right-click-requirement")) {
            MenuRequirementGroup rr = parseRequirementGroup(v.getConfigurationSection("right-click-requirement"));
            if (rr == null) rr = parseRequirementGroup(v.getConfigurationSection("right_click_requirement"));
            if (rr == null) {
                String expr = v.getString("right-click-requirement", "");
                if (expr != null && !expr.isBlank()) rr = simpleJavascriptRequirement(expr);
            }
            if (rr != null) rightClickRequirement = rr;
        }

        return new MenuItemDefinition(base.getSlot(), base.getSlots(), material, name, lore, base.getActions(), conditions,
                base.getClickActions(), amount, glow, modelData, hideAttributes, priority, update, permission,
                viewRequirement, leftClickRequirement, rightClickRequirement,
                itemFlags, enchantments, unbreakable);
    }

    private MenuRequirementGroup parseRequirementGroup(ConfigurationSection sec) {
        if (sec == null) return null;
        int minimum = sec.getInt("minimum", sec.getInt("min", 0));
        List<MenuAction> denyActions = new ArrayList<>();
        for (String raw : sec.getStringList("deny-actions")) {
            denyActions.add(MenuAction.parse(raw));
        }

        List<MenuRequirement> reqs = new ArrayList<>();
        ConfigurationSection reqSec = sec.getConfigurationSection("requirements");
        if (reqSec != null) {
            for (String key : reqSec.getKeys(false)) {
                ConfigurationSection r = reqSec.getConfigurationSection(key);
                if (r == null) continue;
                String type = r.getString("type", "");
                if (type.isBlank()) continue;
                boolean inverted = r.getBoolean("invert", false);
                if (type.startsWith("!")) {
                    inverted = true;
                    type = type.substring(1);
                }
                boolean optional = r.getBoolean("optional", false);
                Map<String, Object> options = new HashMap<>();
                for (String opt : r.getKeys(false)) {
                    if (opt.equalsIgnoreCase("type") || opt.equalsIgnoreCase("invert")
                            || opt.equalsIgnoreCase("optional")
                            || opt.equalsIgnoreCase("success-actions")
                            || opt.equalsIgnoreCase("deny-actions")) continue;
                    options.put(opt, r.get(opt));
                }

                List<MenuAction> successActions = new ArrayList<>();
                for (String raw : r.getStringList("success-actions")) {
                    successActions.add(MenuAction.parse(raw));
                }
                List<MenuAction> denyActionsReq = new ArrayList<>();
                for (String raw : r.getStringList("deny-actions")) {
                    denyActionsReq.add(MenuAction.parse(raw));
                }

                reqs.add(new MenuRequirement(type, inverted, options, optional, successActions, denyActionsReq));
            }
        }

        return new MenuRequirementGroup(reqs, minimum, denyActions);
    }

    private MenuRequirementGroup simpleJavascriptRequirement(String expression) {
        if (expression == null || expression.isBlank()) return null;
        Map<String, Object> options = new HashMap<>();
        options.put("expression", expression);
        MenuRequirement req = new MenuRequirement("javascript", false, options, false, List.of(), List.of());
        return new MenuRequirementGroup(List.of(req), 0, List.of());
    }
}

