package pb.r1lit.LogicMenu.api;

import pb.r1lit.LogicMenu.gui.dynamic.MenuDynamicProvider;
import pb.r1lit.LogicMenu.gui.model.MenuAction;
import pb.r1lit.LogicMenu.gui.model.MenuCondition;
import pb.r1lit.LogicMenu.gui.model.MenuState;
import org.bukkit.entity.Player;

import java.util.Map;

public interface LogicMenuApi {

    boolean registerAction(String type, MenuActionHandler handler);

    boolean unregisterAction(String type);

    MenuActionHandler getActionHandler(String type);

    boolean registerCondition(String type, ConditionHandler handler);

    boolean unregisterCondition(String type);

    ConditionHandler getConditionHandler(String type);

    boolean registerVarProvider(String id, VarProvider provider);

    boolean unregisterVarProvider(String id);

    void applyVars(Player player, Map<String, String> vars);

    boolean registerDynamicProvider(String key, MenuDynamicProvider provider);

    boolean unregisterDynamicProvider(String key);

    boolean openMenu(Player player, String menuId);

    boolean openMenu(Player player, String menuId, int page);

    String resolveMenuId(String menuOrCommand);

    pb.r1lit.LogicMenu.gui.service.MenuItemFactory getItemFactory();

    pb.r1lit.LogicMenu.gui.service.MenuRequirementService getRequirementService();

    pb.r1lit.LogicMenu.gui.service.MenuConditionService getConditionService();

    pb.r1lit.LogicMenu.gui.service.MenuTextResolver getTextResolver();

    interface MenuActionHandler {
        void execute(Player player, MenuState current, MenuAction action, Map<String, String> vars);
    }

    interface ConditionHandler {
        boolean test(Player player, MenuCondition condition, Map<String, String> vars);
    }

    interface VarProvider {
        void apply(Player player, Map<String, String> vars);
    }
}

