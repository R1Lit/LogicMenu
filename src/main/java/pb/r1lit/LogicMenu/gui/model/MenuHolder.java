package pb.r1lit.LogicMenu.gui.model;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class MenuHolder implements InventoryHolder {
    private final MenuState state;
    private final Map<Integer, List<MenuAction>> actionsBySlot = new HashMap<>();
    private final Map<Integer, Map<String, List<MenuAction>>> clickActionsBySlot = new HashMap<>();
    private final Map<Integer, Map<String, String>> varsBySlot = new HashMap<>();
    private final Map<Integer, MenuItemDefinition> defBySlot = new HashMap<>();
    private final Set<Integer> updateSlots = new HashSet<>();
    private Inventory inventory;
    private int lastRenderTick;

    public MenuHolder(MenuState state) {
        this.state = state;
    }

    public MenuState getState() {
        return state;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setSlotActions(int slot, List<MenuAction> actions) {
        actionsBySlot.put(slot, actions);
    }

    public List<MenuAction> getSlotActions(int slot) {
        return actionsBySlot.get(slot);
    }

    public void setSlotClickActions(int slot, Map<String, List<MenuAction>> actions) {
        if (actions != null && !actions.isEmpty()) {
            clickActionsBySlot.put(slot, actions);
        }
    }

    public Map<String, List<MenuAction>> getSlotClickActions(int slot) {
        return clickActionsBySlot.get(slot);
    }

    public void setSlotVars(int slot, Map<String, String> vars) {
        varsBySlot.put(slot, vars);
    }

    public Map<String, String> getSlotVars(int slot) {
        return varsBySlot.get(slot);
    }

    public void setSlotDefinition(int slot, MenuItemDefinition def) {
        defBySlot.put(slot, def);
        if (def != null && def.isUpdate()) {
            updateSlots.add(slot);
        }
    }

    public MenuItemDefinition getSlotDefinition(int slot) {
        return defBySlot.get(slot);
    }

    public Set<Integer> getUpdateSlots() {
        return updateSlots;
    }

    public int getLastRenderTick() {
        return lastRenderTick;
    }

    public void setLastRenderTick(int lastRenderTick) {
        this.lastRenderTick = lastRenderTick;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

