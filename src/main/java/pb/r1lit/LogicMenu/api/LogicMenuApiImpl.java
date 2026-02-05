package pb.r1lit.LogicMenu.api;

import pb.r1lit.LogicMenu.gui.dynamic.MenuDynamicProvider;
import pb.r1lit.LogicMenu.gui.core.MenuEngine;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogicMenuApiImpl implements LogicMenuApi {
    private final Map<String, MenuActionHandler> actions = new ConcurrentHashMap<>();
    private final Map<String, ConditionHandler> conditions = new ConcurrentHashMap<>();
    private final Map<String, VarProvider> varProviders = new ConcurrentHashMap<>();
    private final MenuEngine menus;

    public LogicMenuApiImpl(MenuEngine menus) {
        this.menus = menus;
    }

    @Override
    public boolean registerAction(String type, MenuActionHandler handler) {
        if (type == null || type.isBlank() || handler == null) return false;
        actions.put(type.toUpperCase(Locale.ROOT), handler);
        return true;
    }

    @Override
    public boolean unregisterAction(String type) {
        if (type == null || type.isBlank()) return false;
        return actions.remove(type.toUpperCase(Locale.ROOT)) != null;
    }

    @Override
    public MenuActionHandler getActionHandler(String type) {
        if (type == null || type.isBlank()) return null;
        return actions.get(type.toUpperCase(Locale.ROOT));
    }

    @Override
    public boolean registerCondition(String type, ConditionHandler handler) {
        if (type == null || type.isBlank() || handler == null) return false;
        conditions.put(type.toUpperCase(Locale.ROOT), handler);
        return true;
    }

    @Override
    public boolean unregisterCondition(String type) {
        if (type == null || type.isBlank()) return false;
        return conditions.remove(type.toUpperCase(Locale.ROOT)) != null;
    }

    @Override
    public ConditionHandler getConditionHandler(String type) {
        if (type == null || type.isBlank()) return null;
        return conditions.get(type.toUpperCase(Locale.ROOT));
    }

    @Override
    public boolean registerVarProvider(String id, VarProvider provider) {
        if (id == null || id.isBlank() || provider == null) return false;
        varProviders.put(id.toUpperCase(Locale.ROOT), provider);
        return true;
    }

    @Override
    public boolean unregisterVarProvider(String id) {
        if (id == null || id.isBlank()) return false;
        return varProviders.remove(id.toUpperCase(Locale.ROOT)) != null;
    }

    @Override
    public void applyVars(org.bukkit.entity.Player player, Map<String, String> vars) {
        var iterator = varProviders.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, VarProvider> entry = iterator.next();
            try {
                entry.getValue().apply(player, vars);
            } catch (Throwable t) {
                iterator.remove();
                try {
                    var logger = pb.r1lit.LogicMenu.LogicMenu.getInstance() != null
                            ? pb.r1lit.LogicMenu.LogicMenu.getInstance().getLogger()
                            : java.util.logging.Logger.getLogger("LogicMenu");
                    logger.warning("[LogicMenu] Removed var provider '" + entry.getKey() + "' due to error: " + t.getMessage());
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public boolean registerDynamicProvider(String key, MenuDynamicProvider provider) {
        if (menus == null) return false;
        return menus.registerDynamicProvider(key, provider);
    }

    @Override
    public boolean unregisterDynamicProvider(String key) {
        if (menus == null) return false;
        return menus.unregisterDynamicProvider(key);
    }

    @Override
    public boolean openMenu(org.bukkit.entity.Player player, String menuId) {
        if (menus == null || player == null || menuId == null) return false;
        menus.openMenu(player, menuId);
        return true;
    }

    @Override
    public boolean openMenu(org.bukkit.entity.Player player, String menuId, int page) {
        if (menus == null || player == null || menuId == null) return false;
        menus.openMenu(player, menuId, page);
        return true;
    }

    @Override
    public String resolveMenuId(String menuOrCommand) {
        if (menus == null) return null;
        return menus.resolveMenuId(menuOrCommand);
    }

    @Override
    public pb.r1lit.LogicMenu.gui.service.MenuItemFactory getItemFactory() {
        return menus != null ? menus.getItemFactory() : null;
    }

    @Override
    public pb.r1lit.LogicMenu.gui.service.MenuRequirementService getRequirementService() {
        return menus != null ? menus.getRequirementService() : null;
    }

    @Override
    public pb.r1lit.LogicMenu.gui.service.MenuConditionService getConditionService() {
        return menus != null ? menus.getConditionService() : null;
    }

    @Override
    public pb.r1lit.LogicMenu.gui.service.MenuTextResolver getTextResolver() {
        return menus != null ? menus.getResolver() : null;
    }

    public void clearRegistries() {
        actions.clear();
        conditions.clear();
        varProviders.clear();
        if (menus != null) {
            menus.clearDynamicProviders();
        }
    }
}

