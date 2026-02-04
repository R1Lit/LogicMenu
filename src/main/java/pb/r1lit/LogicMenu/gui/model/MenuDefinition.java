package pb.r1lit.LogicMenu.gui.model;

import java.util.List;

public class MenuDefinition {
    private final String id;
    private final String title;
    private final int size;
    private final List<MenuItemDefinition> items;
    private final MenuItemDefinition fillItem;
    private final java.util.List<Integer> fillSlots;
    private final String fillSlotRange;
    private final MenuDynamicDefinition dynamic;
    private final List<MenuDynamicDefinition> dynamics;
    private final java.util.Map<String, String> vars;
    private final String permission;
    private final boolean update;
    private final int updateIntervalTicks;
    private final MenuRequirementGroup openRequirement;
    private final java.util.List<String> openCommands;

    public MenuDefinition(String id, String title, int size, List<MenuItemDefinition> items,
                          MenuItemDefinition fillItem, MenuDynamicDefinition dynamic,
                          List<MenuDynamicDefinition> dynamics, java.util.Map<String, String> vars,
                          String permission, boolean update, int updateIntervalTicks,
                          MenuRequirementGroup openRequirement,
                          java.util.List<Integer> fillSlots, String fillSlotRange,
                          java.util.List<String> openCommands) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.items = items;
        this.fillItem = fillItem;
        this.fillSlots = fillSlots;
        this.fillSlotRange = fillSlotRange;
        this.dynamic = dynamic;
        this.dynamics = dynamics;
        this.vars = vars;
        this.permission = permission;
        this.update = update;
        this.updateIntervalTicks = updateIntervalTicks;
        this.openRequirement = openRequirement;
        this.openCommands = openCommands;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public List<MenuItemDefinition> getItems() {
        return items;
    }

    public MenuItemDefinition getFillItem() {
        return fillItem;
    }

    public java.util.List<Integer> getFillSlots() {
        return fillSlots;
    }

    public String getFillSlotRange() {
        return fillSlotRange;
    }

    public MenuDynamicDefinition getDynamic() {
        return dynamic;
    }

    public List<MenuDynamicDefinition> getDynamics() {
        return dynamics;
    }

    public java.util.Map<String, String> getVars() {
        return vars;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isUpdate() {
        return update;
    }

    public int getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }

    public MenuRequirementGroup getOpenRequirement() {
        return openRequirement;
    }

    public java.util.List<String> getOpenCommands() {
        return openCommands;
    }
}

