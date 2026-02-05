package pb.r1lit.LogicMenu.gui.model;

import pb.r1lit.LogicMenu.gui.model.types.MenuConditionType;

public class MenuCondition {
    private final MenuConditionType type;
    private final String value;

    public MenuCondition(MenuConditionType type, String value) {
        this.type = type;
        this.value = value;
    }

    public MenuConditionType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public static MenuCondition parse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (!raw.contains(":")) {
            return new MenuCondition(MenuConditionType.EXPRESSION, raw.trim());
        }
        String[] parts = raw.split(":", 2);
        String typeKey = parts[0].trim().toUpperCase(java.util.Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        MenuConditionType type;
        try {
            type = MenuConditionType.valueOf(typeKey);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String value = parts.length > 1 ? parts[1].trim() : "";
        return new MenuCondition(type, value);
    }
}

