package pb.r1lit.LogicMenu.gui.dynamic;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

import pb.r1lit.LogicMenu.gui.model.MenuDefinition;
import pb.r1lit.LogicMenu.gui.model.MenuDynamicDefinition;
import pb.r1lit.LogicMenu.gui.model.MenuHolder;

public interface MenuDynamicProvider {
    void populate(Player player, MenuDefinition menu, MenuDynamicDefinition dynamic,
                  MenuHolder holder, Inventory inventory, Map<String, String> baseVars, int page);
}

