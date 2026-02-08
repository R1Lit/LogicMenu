package pb.r1lit.LogicMenu.command.Sub;

import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.gui.model.MenuHolder;

import java.util.Locale;

public class Reload implements Lm {
    private final LogicMenu plugin;

    public Reload(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return "logicmenu.reload";
    }

    @Override
    public String description() {
        return "Reload config or expansions";
    }

    @Override
    public String usage() {
        return "<config|expansions>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(plugin.getLang().get("reload.usage",
                    "&cUsage: /logicmenu reload <config|expansions>"));
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        if (!mode.equals("config") && !mode.equals("expansions")) {
            sender.sendMessage(plugin.getLang().get("reload.usage",
                    "&cUsage: /logicmenu reload <config|expansions>"));
            return true;
        }

        sender.sendMessage(plugin.getLang().get("reload.started", "&eLogicMenu reload started: {type}")
                .replace("{type}", mode));

        try {
            closeOpenLogicMenus();
            if (mode.equals("config")) {
                plugin.reloadConfig();
                plugin.getLang().reload();
                plugin.registerGuiCommands();
                if (plugin.getMenus() != null) {
                    plugin.getMenus().reload();
                    plugin.getLogger().info(plugin.getLang().get("log.menus_loaded", "Menus loaded: {count}")
                            .replace("{count}", String.valueOf(plugin.getMenus().getMenuCount())));
                }
            } else {
                int loaded = plugin.reloadExpansions();
                sender.sendMessage(plugin.getLang().get("expansions.reload", "&aExpansion scan complete. Loaded: &f{count}")
                        .replace("{count}", String.valueOf(loaded)));
            }
            sender.sendMessage(plugin.getLang().get("reload.done", "&aLogicMenu reloaded: {type}")
                    .replace("{type}", mode));
        } catch (Exception e) {
            sender.sendMessage(plugin.getLang().get("reload.failed", "&cReload failed: {error}")
                    .replace("{error}", e.getMessage()));
        }
        return true;
    }

    private void closeOpenLogicMenus() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                player.closeInventory();
            }
        });
    }
}
