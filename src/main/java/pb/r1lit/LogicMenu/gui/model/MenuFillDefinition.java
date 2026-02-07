package pb.r1lit.LogicMenu.gui.model;

import java.util.List;

public class MenuFillDefinition {
    private final MenuItemDefinition item;
    private final List<Integer> slots;
    private final String slotRange;
    private final String type;
    private final int priority;

    public MenuFillDefinition(MenuItemDefinition item, List<Integer> slots, String slotRange, String type, int priority) {
        this.item = item;
        this.slots = slots;
        this.slotRange = slotRange;
        this.type = type;
        this.priority = priority;
    }

    public MenuItemDefinition getItem() {
        return item;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public String getSlotRange() {
        return slotRange;
    }

    public String getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }
}
