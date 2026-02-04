package pb.r1lit.LogicMenu.gui.model;

import java.util.HashMap;
import java.util.Map;

public class MenuAction {
    private final MenuActionType type;
    private final String value;
    private final Map<String, String> params;

    public MenuAction(MenuActionType type, String value, Map<String, String> params) {
        this.type = type;
        this.value = value;
        this.params = params;
    }

    public MenuActionType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getTypeKey() {
        if (type == MenuActionType.CUSTOM) {
            return params.getOrDefault("_type", "CUSTOM");
        }
        return type.name();
    }

    public static MenuAction parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new MenuAction(MenuActionType.CLOSE, "", Map.of());
        }

        String[] parts = raw.split(":", 2);
        MenuActionType type;
        String rawType = parts[0].trim();
        try {
            type = MenuActionType.valueOf(rawType.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = MenuActionType.CUSTOM;
        }
        String value = parts.length > 1 ? parts[1].trim() : "";

        Map<String, String> params = new HashMap<>();
        if (type == MenuActionType.CUSTOM) {
            params.put("_type", rawType.toUpperCase());
        }
        if ((type == MenuActionType.OPEN_MENU || type == MenuActionType.CUSTOM) && !value.isBlank()) {
            // Format: value;key=value;key2=value2
            String[] tokens = value.split(";");
            if (tokens.length > 0) {
                value = tokens[0].trim();
                for (int i = 1; i < tokens.length; i++) {
                    String token = tokens[i].trim();
                    if (token.isEmpty()) continue;
                    String[] kv = token.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }
        }

        return new MenuAction(type, value, params);
    }
}

