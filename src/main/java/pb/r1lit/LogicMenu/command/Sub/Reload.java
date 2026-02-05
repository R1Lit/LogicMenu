package pb.r1lit.LogicMenu.command.Sub;

import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.command.LmSubCommand;

public class Reload implements LmSubCommand {
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
        return "Reload config and menus";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(plugin.getLang().get("reload.started", "&eLogicMenu reload started..."));
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                // Close open menus to avoid stale holders
                plugin.getServer().getOnlinePlayers().forEach(p -> {
                    if (p.getOpenInventory().getTopInventory().getHolder() instanceof pb.r1lit.LogicMenu.gui.model.MenuHolder) {
                        p.closeInventory();
                    }
                });

                plugin.reloadConfig();
                plugin.getLang().reload();
                plugin.registerGuiCommands();
                if (plugin.getMenus() != null) {
                    plugin.getMenus().reload();
                    plugin.getLogger().info(plugin.getLang().get("log.menus_loaded", "Menus loaded: {count}")
                            .replace("{count}", String.valueOf(plugin.getMenus().getMenuCount())));
                }
                // Re-scan expansions if needed
                plugin.reloadExpansions();
                sender.sendMessage(plugin.getLang().get("reload.done", "&aLogicMenu reloaded."));
            } catch (Exception e) {
                sender.sendMessage(plugin.getLang().get("reload.failed", "&cReload failed: {error}")
                        .replace("{error}", e.getMessage()));
            }
        });
        return true;
    }
}
