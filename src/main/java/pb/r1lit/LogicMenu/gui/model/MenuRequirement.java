package pb.r1lit.LogicMenu.gui.model;

import java.util.Map;

public class MenuRequirement {
    private final String type;
    private final boolean inverted;
    private final Map<String, Object> options;
    private final boolean optional;
    private final java.util.List<MenuAction> successActions;
    private final java.util.List<MenuAction> denyActions;

    public MenuRequirement(String type, boolean inverted, Map<String, Object> options,
                           boolean optional, java.util.List<MenuAction> successActions,
                           java.util.List<MenuAction> denyActions) {
        this.type = type;
        this.inverted = inverted;
        this.options = options;
        this.optional = optional;
        this.successActions = successActions;
        this.denyActions = denyActions;
    }

    public String getType() {
        return type;
    }

    public boolean isInverted() {
        return inverted;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public boolean isOptional() {
        return optional;
    }

    public java.util.List<MenuAction> getSuccessActions() {
        return successActions;
    }

    public java.util.List<MenuAction> getDenyActions() {
        return denyActions;
    }
}

