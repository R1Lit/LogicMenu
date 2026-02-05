package pb.r1lit.LogicMenu.gui.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.api.LogicMenuApi;
import pb.r1lit.LogicMenu.gui.config.MenuConfigLoader;
import pb.r1lit.LogicMenu.gui.dynamic.MenuDynamicProvider;
import pb.r1lit.LogicMenu.gui.model.MenuAction;
import pb.r1lit.LogicMenu.gui.model.MenuDefinition;
import pb.r1lit.LogicMenu.gui.model.MenuDynamicDefinition;
import pb.r1lit.LogicMenu.gui.model.MenuHolder;
import pb.r1lit.LogicMenu.gui.model.MenuItemDefinition;
import pb.r1lit.LogicMenu.gui.model.MenuRequirementGroup;
import pb.r1lit.LogicMenu.gui.model.MenuState;
import pb.r1lit.LogicMenu.gui.service.MenuConditionService;
import pb.r1lit.LogicMenu.gui.service.MenuItemFactory;
import pb.r1lit.LogicMenu.gui.service.MenuRequirementService;
import pb.r1lit.LogicMenu.gui.service.MenuTextResolver;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MenuEngine implements Listener, MenuNavigation {
    private final LogicMenu plugin;
    private final MenuConfigLoader loader = new MenuConfigLoader();
    private final Map<String, MenuDefinition> menus = new HashMap<>();
    private final Map<String, MenuDynamicProvider> providers = new HashMap<>();
    private final Map<UUID, Deque<MenuState>> history = new HashMap<>();
    private final Map<String, String> openCommandIndex = new HashMap<>();
    private final Map<String, String> pathIndex = new HashMap<>();
    private LogicMenuApi api;
    private final MenuTextResolver resolver = new MenuTextResolver();
    private final MenuItemFactory itemFactory;
    private final MenuActionExecutor actionExecutor;
    private final MenuRequirementService requirementService;
    private final MenuConditionService conditionService;
    private int tickCounter = 0;

    public MenuEngine(LogicMenu plugin, pb.r1lit.LogicMenu.gui.service.MenuItemMarker marker) {
        this.plugin = plugin;
        this.itemFactory = new MenuItemFactory(resolver, marker);
        this.actionExecutor = new MenuActionExecutor(plugin, resolver, this);
        this.requirementService = new MenuRequirementService(plugin, resolver, actionExecutor);
        this.conditionService = new MenuConditionService(resolver, null);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickCounter++;
            refreshOpenMenus();
        }, 20L, 20L);
    }

    public void setApi(LogicMenuApi api) {
        this.api = api;
        this.conditionService.setApi(api);
    }

    public boolean registerDynamicProvider(String key, MenuDynamicProvider provider) {
        if (key == null || key.isBlank() || provider == null) return false;
        providers.put(key.toUpperCase(Locale.ROOT), provider);
        return true;
    }

    public boolean unregisterDynamicProvider(String key) {
        if (key == null || key.isBlank()) return false;
        return providers.remove(key.toUpperCase(Locale.ROOT)) != null;
    }

    public void clearDynamicProviders() {
        providers.clear();
    }

    public MenuItemFactory getItemFactory() {
        return itemFactory;
    }

    public MenuRequirementService getRequirementService() {
        return requirementService;
    }

    public MenuConditionService getConditionService() {
        return conditionService;
    }

    public MenuTextResolver getResolver() {
        return resolver;
    }

    public List<String> getMenuIds() {
        return new ArrayList<>(menus.keySet());
    }

    public int getMenuCount() {
        return menus.size();
    }

    public MenuDefinition getMenuDefinition(String id) {
        return menus.get(id);
    }

    public Map<String, String> createBaseVars(Player player) {
        return buildBaseVars(player, 0);
    }

    public void executeAction(Player player, MenuAction action, Map<String, String> vars) {
        if (player == null || action == null) return;
        Map<String, String> safeVars = vars == null ? Map.of() : vars;
        actionExecutor.execute(player, new MenuState("", 0, safeVars), action, safeVars);
    }

    public void reload() {
        menus.clear();
        openCommandIndex.clear();
        pathIndex.clear();
        menus.putAll(loadFromFiles());
        if (menus.isEmpty()) {
            FileConfiguration config = plugin.getConfig();
            menus.putAll(loader.load(config));
        }
        indexGuiCommandAliases();
        for (MenuDefinition def : menus.values()) {
            if (def.getOpenCommands() == null) continue;
            for (String cmd : def.getOpenCommands()) {
                if (cmd == null || cmd.isBlank()) continue;
                openCommandIndex.put(cmd.toLowerCase(Locale.ROOT), def.getId());
            }
        }
    }

    public String resolveMenuId(String menuOrCommand) {
        if (menuOrCommand == null) return null;
        String key = normalizeMenuKey(menuOrCommand);
        if (pathIndex.containsKey(key)) return pathIndex.get(key);
        if (menus.containsKey(key)) return key;
        return openCommandIndex.getOrDefault(key, key);
    }

    private Map<String, MenuDefinition> loadFromFiles() {
        Map<String, MenuDefinition> result = new HashMap<>();
        File folder = new File(plugin.getDataFolder(), "gui");
        if (!folder.exists() || !folder.isDirectory()) return result;

        List<File> files = new ArrayList<>();
        collectYamlFiles(folder, files);
        if (files.isEmpty()) return result;

        for (File file : files) {
            String relPath = toRelativePath(folder, file);
            if (relPath == null) continue;
            String id = stripYamlExtension(relPath);
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            result.putAll(loader.loadSingle(cfg, id));
            String relKey = normalizeMenuKey(relPath);
            String idKey = normalizeMenuKey(id);
            pathIndex.put(relKey, id);
            pathIndex.put(idKey, id);
        }
        return result;
    }

    private void indexGuiCommandAliases() {
        FileConfiguration cfg = plugin.getConfig();
        if (cfg == null) return;
        var section = cfg.getConfigurationSection("gui-commands");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            var cmdSec = section.getConfigurationSection(key);
            if (cmdSec == null) continue;
            String menu = cmdSec.getString("menu", "");
            if (menu == null || menu.isBlank()) continue;
            String menuKey = normalizeMenuKey(menu);
            String id = pathIndex.getOrDefault(menuKey, menuKey);
            pathIndex.put(normalizeMenuKey(key), id);
        }
    }

    public void openMenu(Player player, String menuId) {
        openMenu(player, menuId, 0, Map.of(), false, null);
    }

    public void openMenu(Player player, String menuId, int page) {
        openMenu(player, menuId, page, Map.of(), false, null);
    }

    @Override
    public void openMenu(Player player, String menuId, int page, Map<String, String> vars, boolean pushHistory, MenuState current) {
        String resolvedId = resolveMenuId(menuId);
        if (resolvedId == null) resolvedId = menuId;
        MenuDefinition menu = menus.get(resolvedId);
        if (menu == null) {
            player.sendMessage(plugin.getLang().get("menu.not_found", "&cMenu not found: {menu}")
                    .replace("{menu}", menuId));
            return;
        }

        if (menu.getPermission() != null && !menu.getPermission().isBlank()) {
            if (!player.hasPermission(menu.getPermission())) {
                player.sendMessage(plugin.getLang().get("menu.no_access", "&cNo access."));
                return;
            }
        }

        if (menu.getOpenRequirement() != null) {
            Map<String, String> openVars = buildVars(player, menu, new MenuState(menuId, page, vars));
            if (!requirementService.checkRequirementGroup(player, menu.getOpenRequirement(), openVars)) {
                requirementService.runDenyActions(player, menu.getOpenRequirement(), openVars);
                return;
            }
        }

        if (pushHistory && current != null) {
            history.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).push(current);
        }

        MenuState state = new MenuState(resolvedId, page, vars);
        MenuHolder holder = new MenuHolder(state);

        renderMenu(player, menu, holder);
        player.openInventory(holder.getInventory());
    }

    @Override
    public void openBack(Player player) {
        Deque<MenuState> stack = history.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            player.closeInventory();
            return;
        }
        MenuState prev = stack.pop();
        openMenu(player, prev.getMenuId(), prev.getPage(), prev.getCustomVars(), false, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);
        if (event.getClick().isShiftClick()) {
            player.setItemOnCursor(null);
            player.updateInventory();
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getSlot();
        List<MenuAction> actions = holder.getSlotActions(slot);
        Map<String, List<MenuAction>> clickMap = holder.getSlotClickActions(slot);
        if ((actions == null || actions.isEmpty()) && (clickMap == null || clickMap.isEmpty())) return;

        MenuDefinition menu = menus.get(holder.getState().getMenuId());
        if (menu == null) return;

        Map<String, String> baseVars = buildVars(player, menu, holder.getState());
        Map<String, String> slotVars = holder.getSlotVars(slot);
        Map<String, String> vars = new HashMap<>(baseVars);
        if (slotVars != null) vars.putAll(slotVars);

        MenuItemDefinition def = holder.getSlotDefinition(slot);
        if (def != null) {
            MenuRequirementGroup clickReq = null;
            if (event.getClick().isLeftClick()) clickReq = def.getLeftClickRequirement();
            if (event.getClick().isRightClick()) clickReq = def.getRightClickRequirement();
            if (clickReq != null && !requirementService.checkRequirementGroup(player, clickReq, vars)) {
                requirementService.runDenyActions(player, clickReq, vars);
                return;
            }
        }

        List<MenuAction> clickSpecific = resolveClickActions(actions == null ? List.of() : actions, holder, slot, event.getClick());
        if (clickSpecific == null || clickSpecific.isEmpty()) return;
        for (MenuAction action : clickSpecific) {
            actionExecutor.execute(player, holder.getState(), action, vars);
        }

        player.setItemOnCursor(null);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) return;
        player.setItemOnCursor(null);
    }

    private void renderMenu(Player player, MenuDefinition menu, MenuHolder holder) {
        Map<String, String> baseVars = buildVars(player, menu, holder.getState());

        String title = resolver.resolve(menu.getTitle(), player, baseVars);
        Inventory inv = Bukkit.createInventory(holder, menu.getSize(), title);
        holder.setInventory(inv);

        Map<Integer, List<MenuItemDefinition>> bySlot = mapItemsBySlot(menu.getItems(), inv.getSize());
        for (Map.Entry<Integer, List<MenuItemDefinition>> entry : bySlot.entrySet()) {
            int slot = entry.getKey();
            MenuItemDefinition def = chooseItemForSlot(player, menu, entry.getValue(), baseVars, null);
            if (def == null) continue;

            ItemStack item = itemFactory.buildItem(def, player, baseVars, null);
            if (item == null) continue;

            inv.setItem(slot, item);
            holder.setSlotActions(slot, def.getActions());
            holder.setSlotClickActions(slot, def.getClickActions());
            holder.setSlotDefinition(slot, def);
        }

        if (menu.getDynamic() != null) {
            MenuDynamicProvider provider = providers.get(menu.getDynamic().getProvider().toUpperCase(Locale.ROOT));
            if (provider != null) {
                provider.populate(player, menu, menu.getDynamic(), holder, inv, baseVars, holder.getState().getPage());
            }
        }
        if (menu.getDynamics() != null) {
            for (MenuDynamicDefinition dyn : menu.getDynamics()) {
                MenuDynamicProvider provider = providers.get(dyn.getProvider().toUpperCase(Locale.ROOT));
                if (provider != null) {
                    provider.populate(player, menu, dyn, holder, inv, baseVars, holder.getState().getPage());
                }
            }
        }

        if (menu.getFillItem() != null) {
            ItemStack fill = itemFactory.buildItem(menu.getFillItem(), player, baseVars, null);
            if (fill != null) {
                List<Integer> slots = resolveFillSlots(menu, inv.getSize());
                if (slots.isEmpty()) {
                    for (int i = 0; i < inv.getSize(); i++) {
                        if (inv.getItem(i) == null) {
                            inv.setItem(i, fill);
                        }
                    }
                } else {
                    for (int slot : slots) {
                        if (slot < 0 || slot >= inv.getSize()) continue;
                        if (inv.getItem(slot) == null) {
                            inv.setItem(slot, fill);
                        }
                    }
                }
            }
        }

        holder.setLastRenderTick(tickCounter);
    }

    private Map<Integer, List<MenuItemDefinition>> mapItemsBySlot(List<MenuItemDefinition> items, int size) {
        Map<Integer, List<MenuItemDefinition>> bySlot = new HashMap<>();
        for (MenuItemDefinition def : items) {
            List<Integer> slots = new ArrayList<>();
            if (def.getSlots() != null && !def.getSlots().isEmpty()) {
                slots.addAll(def.getSlots());
            } else if (def.getSlot() >= 0) {
                slots.add(def.getSlot());
            }
            for (int slot : slots) {
                if (slot < 0 || slot >= size) continue;
                bySlot.computeIfAbsent(slot, k -> new ArrayList<>()).add(def);
            }
        }
        return bySlot;
    }

    private MenuItemDefinition chooseItemForSlot(Player player, MenuDefinition menu,
                                                List<MenuItemDefinition> defs, Map<String, String> baseVars,
                                                Map<String, String> extraVars) {
        List<MenuItemDefinition> sorted = defs.stream()
                .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .collect(Collectors.toList());

        for (MenuItemDefinition def : sorted) {
            if (!conditionService.passesConditions(player, def.getConditions(), baseVars, extraVars)) continue;

            if (def.getPermission() != null && !def.getPermission().isBlank()) {
                if (!player.hasPermission(def.getPermission())) continue;
            }

            if (def.getViewRequirement() != null) {
                Map<String, String> merged = new HashMap<>(baseVars);
                if (extraVars != null) merged.putAll(extraVars);
                if (!requirementService.checkRequirementGroup(player, def.getViewRequirement(), merged)) {
                    continue;
                }
            }

            return def;
        }
        return null;
    }

    private Map<String, String> buildVars(Player player, MenuDefinition menu, MenuState state) {
        Map<String, String> vars = buildBaseVars(player, state.getPage());
        for (Map.Entry<String, String> entry : menu.getVars().entrySet()) {
            vars.put(entry.getKey(), resolver.resolve(entry.getValue(), player, vars));
        }
        vars.putAll(state.getCustomVars());
        return vars;
    }

    private void refreshOpenMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder)) continue;
            MenuDefinition menu = menus.get(holder.getState().getMenuId());
            if (menu == null) continue;
            if (!menu.isUpdate() && holder.getUpdateSlots().isEmpty()) continue;

            int interval = menu.getUpdateIntervalTicks();
            if (interval <= 0) continue;

            if (tickCounter - holder.getLastRenderTick() < interval) continue;

            refreshMenu(player, menu, holder);
        }
    }

    private void refreshMenu(Player player, MenuDefinition menu, MenuHolder holder) {
        Map<String, String> baseVars = buildVars(player, menu, holder.getState());
        Inventory inv = holder.getInventory();
        if (inv == null) return;

        Map<Integer, List<MenuItemDefinition>> bySlot = mapItemsBySlot(menu.getItems(), inv.getSize());
        for (int slot : holder.getUpdateSlots()) {
            List<MenuItemDefinition> defs = bySlot.get(slot);
            if (defs == null) continue;
            MenuItemDefinition def = chooseItemForSlot(player, menu, defs, baseVars, holder.getSlotVars(slot));
            if (def == null) continue;
            ItemStack item = itemFactory.buildItem(def, player, baseVars, holder.getSlotVars(slot));
            if (item == null) continue;
            inv.setItem(slot, item);
            holder.setSlotActions(slot, def.getActions());
            holder.setSlotClickActions(slot, def.getClickActions());
            holder.setSlotDefinition(slot, def);
        }

        holder.setLastRenderTick(tickCounter);
    }

    private List<Integer> resolveFillSlots(MenuDefinition menu, int size) {
        List<Integer> slots = new ArrayList<>();
        if (menu.getFillSlots() != null && !menu.getFillSlots().isEmpty()) {
            slots.addAll(menu.getFillSlots());
        }
        String range = menu.getFillSlotRange();
        if (range != null && !range.isBlank()) {
            String[] parts = range.split("-");
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());
                    for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                        slots.add(i);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return slots;
    }

    private Map<String, String> buildBaseVars(Player player, int page) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", player.getName());
        vars.put("page", String.valueOf(page + 1));
        vars.put("page_index", String.valueOf(page));
        vars.put("page_next", String.valueOf(page + 2));
        vars.put("page_prev", String.valueOf(Math.max(1, page)));
        if (plugin.getAnchorStore() != null) {
            Map<String, String> anchors = plugin.getAnchorStore().getAll(player);
            for (Map.Entry<String, String> entry : anchors.entrySet()) {
                vars.putIfAbsent(entry.getKey(), entry.getValue());
                vars.putIfAbsent("anchor_" + entry.getKey(), entry.getValue());
            }
        }
        if (api != null) {
            api.applyVars(player, vars);
        }
        return vars;
    }

    private List<MenuAction> resolveClickActions(List<MenuAction> fallback, MenuHolder holder, int slot, ClickType click) {
        Map<String, List<MenuAction>> clickMap = holder.getSlotClickActions(slot);
        if (clickMap != null && !clickMap.isEmpty()) {
            String key = toClickKey(click);
            List<MenuAction> list = clickMap.get(key);
            if (list != null && !list.isEmpty()) return list;
        }
        return fallback;
    }

    private String toClickKey(ClickType click) {
        if (click == null) return "left";
        if (click == ClickType.LEFT) return "left";
        if (click == ClickType.RIGHT) return "right";
        if (click == ClickType.SHIFT_LEFT) return "shift_left";
        if (click == ClickType.SHIFT_RIGHT) return "shift_right";
        if (click == ClickType.MIDDLE) return "middle";
        if (click == ClickType.DOUBLE_CLICK) return "double";
        if (click == ClickType.DROP) return "drop";
        if (click == ClickType.NUMBER_KEY) return "hotbar";
        return "left";
    }

    private void collectYamlFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                collectYamlFiles(child, out);
            } else if (child.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                out.add(child);
            }
        }
    }

    private String toRelativePath(File root, File file) {
        try {
            String rel = root.toPath().relativize(file.toPath()).toString();
            return rel.replace('\\', '/');
        } catch (Exception e) {
            return null;
        }
    }

    private String stripYamlExtension(String path) {
        if (path == null) return null;
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".yml")) {
            return path.substring(0, path.length() - 4);
        }
        return path;
    }

    private String normalizeMenuKey(String key) {
        if (key == null) return null;
        String out = key.trim().replace('\\', '/');
        if (out.startsWith("./")) out = out.substring(2);
        return out.toLowerCase(Locale.ROOT);
    }

}
