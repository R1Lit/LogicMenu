package pb.r1lit.LogicMenu.gui.core;

import org.bukkit.entity.Player;
import pb.r1lit.LogicMenu.gui.model.MenuState;
import java.util.Map;

public interface MenuNavigation {
    void openMenu(Player player, String menuId, int page, Map<String, String> vars, boolean pushHistory, MenuState current);
    void openBack(Player player);
}


