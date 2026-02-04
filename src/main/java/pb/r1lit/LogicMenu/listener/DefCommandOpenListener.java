package pb.r1lit.LogicMenu.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.api.LogicMenuApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DefCommandOpenListener implements Listener {
    private final LogicMenu plugin;
    private final LogicMenuApi api;

    public DefCommandOpenListener(LogicMenu plugin, LogicMenuApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        String message = event.getMessage();
        if (message == null || message.isBlank()) return;
        if (!message.startsWith("/")) return;

        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0) return;

        String cmd = parts[0].toLowerCase(Locale.ROOT);
        int colon = cmd.indexOf(':');
        if (colon >= 0) cmd = cmd.substring(colon + 1);

        boolean debug = plugin.getConfig().getBoolean("debug.commands", false);

        var section = plugin.getConfig().getConfigurationSection("gui-commands");
        if (section == null) {
            if (debug) {
                plugin.getLogger().info("[debug] gui-commands section not found in config.");
            }
            return;
        }

        for (String key : section.getKeys(false)) {
            var cmdSec = section.getConfigurationSection(key);
            if (cmdSec == null) continue;

            String typeRaw = cmdSec.getString("type", "DEF");
            String type = typeRaw == null ? "DEF" : typeRaw.toUpperCase(Locale.ROOT);
            if (!"DEF".equals(type)) continue;

            List<String> commands = new ArrayList<>();
            Object rawCommands = cmdSec.get("commands");
            if (rawCommands instanceof List<?> list) {
                for (Object c : list) {
                    if (c == null) continue;
                    String value = String.valueOf(c).trim();
                    if (!value.isBlank()) commands.add(value.toLowerCase(Locale.ROOT));
                }
            } else {
                String commandsRaw = cmdSec.getString("commands", "");
                if (commandsRaw != null && !commandsRaw.isBlank()) {
                    for (String part : commandsRaw.split(",")) {
                        String value = part.trim();
                        if (!value.isBlank()) commands.add(value.toLowerCase(Locale.ROOT));
                    }
                }
                String single = cmdSec.getString("command", "");
                if (single != null && !single.isBlank()) commands.add(single.toLowerCase(Locale.ROOT));
            }

            if (commands.isEmpty()) continue;
            if (!commands.contains(cmd)) continue;

            boolean ignoreArgs = cmdSec.getBoolean("ignoreargs", false);
            if (!ignoreArgs && parts.length > 1) {
                if (debug) {
                    plugin.getLogger().info("[debug] opener '" + key + "' matched '/" + cmd + "' but args present, ignoreargs=false.");
                }
                return;
            }

            String menuId = cmdSec.getString("menu", "");
            if (menuId == null || menuId.isBlank()) return;

            String permission = cmdSec.getString("permission", "");
            Player player = event.getPlayer();
            if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
                if (debug) {
                    plugin.getLogger().info("[debug] opener '" + key + "' matched but no permission: " + permission);
                }
                player.sendMessage(plugin.getLang().get("command.no_permission", "&cNo permission."));
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            if (api != null) {
                if (debug) {
                    plugin.getLogger().info("[debug] opener '" + key + "' opening menu: " + menuId);
                }
                api.openMenu(player, menuId, 0);
            }
            return;
        }

        if (debug) {
            plugin.getLogger().info("[debug] no opener matched '/" + cmd + "'.");
        }
    }
}
