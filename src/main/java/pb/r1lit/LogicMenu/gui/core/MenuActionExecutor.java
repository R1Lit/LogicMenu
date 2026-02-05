package pb.r1lit.LogicMenu.gui.core;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.gui.model.MenuAction;
import pb.r1lit.LogicMenu.gui.model.MenuState;
import pb.r1lit.LogicMenu.api.LogicMenuApi;
import pb.r1lit.LogicMenu.gui.service.MenuTextResolver;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MenuActionExecutor {
    private final LogicMenu plugin;
    private final MenuTextResolver resolver;
    private final MenuNavigation navigation;

    public MenuActionExecutor(LogicMenu plugin, MenuTextResolver resolver, MenuNavigation navigation) {
        this.plugin = plugin;
        this.resolver = resolver;
        this.navigation = navigation;
    }

    public void execute(Player player, MenuState current, MenuAction action, Map<String, String> vars) {
        if (action == null) return;
        if (shouldSkipByChance(action)) return;
        if (scheduleByDelay(player, current, action, vars)) return;

        switch (action.getType()) {
            case COMMAND -> player.performCommand(resolver.resolve(action.getValue(), player, vars));
            case CONSOLE_COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolver.resolve(action.getValue(), player, vars));
            case MESSAGE -> player.sendMessage(resolver.resolve(action.getValue(), player, vars));
            case ACTIONBAR -> player.sendActionBar(resolver.resolve(action.getValue(), player, vars));
            case TITLE -> {
                String raw = resolver.resolve(action.getValue(), player, vars);
                String[] seg = raw.split(";");
                String[] parts = seg[0].split("\\|", 2);
                String title = resolver.color(parts.length > 0 ? parts[0] : "");
                String subtitle = resolver.color(parts.length > 1 ? parts[1] : "");
                int fadeIn = seg.length > 1 ? (int) parseFloat(seg[1], 10) : 10;
                int stay = seg.length > 2 ? (int) parseFloat(seg[2], 40) : 40;
                int fadeOut = seg.length > 3 ? (int) parseFloat(seg[3], 10) : 10;
                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
            case SOUND -> {
                String[] parts = resolver.resolve(action.getValue(), player, vars).split(";");
                String soundName = parts.length > 0 ? parts[0] : "UI_BUTTON_CLICK";
                float volume = parts.length > 1 ? parseFloat(parts[1], 1.0f) : 1.0f;
                float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0f) : 1.0f;
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException ignored) {
                }
            }
            case BROADCAST_SOUND -> {
                String[] parts = resolver.resolve(action.getValue(), player, vars).split(";");
                String soundName = parts.length > 0 ? parts[0] : "UI_BUTTON_CLICK";
                float volume = parts.length > 1 ? parseFloat(parts[1], 1.0f) : 1.0f;
                float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0f) : 1.0f;
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), sound, volume, pitch);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            case BROADCAST -> {
                String msg = resolver.resolve(action.getValue(), player, vars);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(msg);
                }
            }
            case MINIMESSAGE -> {
                String msg = resolver.resolve(action.getValue(), player, vars);
                player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
            }
            case MINIBROADCAST -> {
                String msg = resolver.resolve(action.getValue(), player, vars);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                }
            }
            case PLACEHOLDER -> resolver.resolve(action.getValue(), player, vars);
            case GIVE_EXP -> {
                Integer amount = parseInt(resolver.resolve(action.getValue(), player, vars));
                if (amount != null) {
                    player.giveExp(amount);
                }
            }
            case TAKE_EXP -> {
                Integer amount = parseInt(resolver.resolve(action.getValue(), player, vars));
                if (amount != null) {
                    int newExp = Math.max(0, player.getTotalExperience() - amount);
                    player.setTotalExperience(0);
                    player.setLevel(0);
                    player.setExp(0);
                    player.giveExp(newExp);
                }
            }
            case CLOSE -> player.closeInventory();
            case BACK -> navigation.openBack(player);
            case OPEN_MENU -> {
                String targetMenu = resolver.resolve(action.getValue(), player, vars);
                int page = 0;
                Map<String, String> newVars = new HashMap<>();
                if (!action.getParams().isEmpty()) {
                    for (Map.Entry<String, String> entry : action.getParams().entrySet()) {
                        String key = entry.getKey();
                        String value = resolver.resolve(entry.getValue(), player, vars);
                        if (key.equalsIgnoreCase("page")) {
                            page = parsePage(value, current != null ? current.getPage() : 0);
                        } else {
                            newVars.put(key, value);
                        }
                    }
                }
                navigation.openMenu(player, targetMenu, page, newVars, true, current);
            }
            case CUSTOM -> {
                LogicMenuApi api = plugin.getApi();
                if (api == null) return;
                LogicMenuApi.MenuActionHandler handler = api.getActionHandler(action.getTypeKey());
                if (handler != null) {
                    try {
                        handler.execute(player, current, action, vars);
                    } catch (Throwable t) {
                        if (api instanceof pb.r1lit.LogicMenu.api.LogicMenuApiImpl impl) {
                            impl.unregisterAction(action.getTypeKey());
                        }
                    }
                }
            }
        }
    }

    private int parsePage(String value, int currentPage) {
        try {
            if (value.startsWith("+") || value.startsWith("-")) {
                return Math.max(0, currentPage + Integer.parseInt(value));
            }
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean shouldSkipByChance(MenuAction action) {
        String chanceRaw = action.getParams().get("_chance");
        if (chanceRaw == null || chanceRaw.isBlank()) return false;
        Double chance = parseDouble(chanceRaw);
        if (chance == null) return false;
        if (chance >= 100.0) return false;
        if (chance <= 0.0) return true;
        double roll = Math.random() * 100.0;
        return roll > chance;
    }

    private boolean scheduleByDelay(Player player, MenuState current, MenuAction action, Map<String, String> vars) {
        String delayRaw = action.getParams().get("_delay");
        if (delayRaw == null || delayRaw.isBlank()) return false;
        if ("true".equalsIgnoreCase(action.getParams().get("_delay_applied"))) return false;
        Integer delay = parseInt(delayRaw);
        if (delay == null || delay <= 0) return false;
        Map<String, String> newParams = new HashMap<>(action.getParams());
        newParams.put("_delay_applied", "true");
        MenuAction delayed = new MenuAction(action.getType(), action.getValue(), newParams);
        Bukkit.getScheduler().runTaskLater(plugin, () -> execute(player, current, delayed, vars), delay);
        return true;
    }
}
