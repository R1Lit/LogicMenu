package pb.r1lit.LogicMenu.gui.model;

import java.util.ArrayList;
import java.util.List;

public class MenuItemDefinition {
    private final int slot;
    private final List<Integer> slots;
    private final String material;
    private final String name;
    private final List<String> lore;
    private final List<MenuAction> actions;
    private final List<MenuCondition> conditions;
    private final java.util.Map<String, List<MenuAction>> clickActions;
    private final int amount;
    private final boolean glow;
    private final Integer modelData;
    private final boolean hideAttributes;
    private final int priority;
    private final boolean update;
    private final String permission;
    private final MenuRequirementGroup viewRequirement;
    private final MenuRequirementGroup leftClickRequirement;
    private final MenuRequirementGroup rightClickRequirement;
    private final java.util.List<String> itemFlags;
    private final java.util.List<String> enchantments;
    private final boolean unbreakable;

    public MenuItemDefinition(int slot, List<Integer> slots, String material, String name, List<String> lore,
                              List<MenuAction> actions, List<MenuCondition> conditions,
                              java.util.Map<String, List<MenuAction>> clickActions,
                              int amount, boolean glow, Integer modelData, boolean hideAttributes,
                              int priority, boolean update, String permission,
                              MenuRequirementGroup viewRequirement,
                              MenuRequirementGroup leftClickRequirement,
                              MenuRequirementGroup rightClickRequirement,
                              java.util.List<String> itemFlags,
                              java.util.List<String> enchantments,
                              boolean unbreakable) {
        this.slot = slot;
        this.slots = slots;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.actions = actions;
        this.conditions = conditions;
        this.clickActions = clickActions;
        this.amount = amount;
        this.glow = glow;
        this.modelData = modelData;
        this.hideAttributes = hideAttributes;
        this.priority = priority;
        this.update = update;
        this.permission = permission;
        this.viewRequirement = viewRequirement;
        this.leftClickRequirement = leftClickRequirement;
        this.rightClickRequirement = rightClickRequirement;
        this.itemFlags = itemFlags;
        this.enchantments = enchantments;
        this.unbreakable = unbreakable;
    }

    public int getSlot() {
        return slot;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public String getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<MenuAction> getActions() {
        return actions;
    }

    public List<MenuCondition> getConditions() {
        return conditions;
    }

    public java.util.Map<String, List<MenuAction>> getClickActions() {
        return clickActions;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isGlow() {
        return glow;
    }

    public Integer getModelData() {
        return modelData;
    }

    public boolean isHideAttributes() {
        return hideAttributes;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isUpdate() {
        return update;
    }

    public String getPermission() {
        return permission;
    }

    public MenuRequirementGroup getViewRequirement() {
        return viewRequirement;
    }

    public MenuRequirementGroup getLeftClickRequirement() {
        return leftClickRequirement;
    }

    public MenuRequirementGroup getRightClickRequirement() {
        return rightClickRequirement;
    }

    public java.util.List<String> getItemFlags() {
        return itemFlags;
    }

    public java.util.List<String> getEnchantments() {
        return enchantments;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public static MenuItemDefinition emptyFill(String material, String name, List<String> lore) {
        return new MenuItemDefinition(-1, List.of(), material, name, lore, new ArrayList<>(),
                new ArrayList<>(), new java.util.HashMap<>(), 1, false, null, true,
                0, false, "", null, null, null,
                new ArrayList<>(), new ArrayList<>(), false);
    }
}

