package pb.r1lit.LogicMenu.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;

public class ReloadCommand implements CommandExecutor {

    private final LogicMenu plugin;

    public ReloadCommand(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("pbg.reload")) {
            sender.sendMessage("§cУ вас нет прав на эту команду.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("open")) {
            if (args.length < 2) {
                sender.sendMessage("§cИспользование: /pbg open <menu|alias> [player]");
                return true;
            }
            if (plugin.getMenus() == null) {
                sender.sendMessage("§cСистема меню не загружена.");
                return true;
            }

            String menuId = plugin.getMenus().resolveMenuId(args[1]);
            org.bukkit.entity.Player target = null;
            if (args.length >= 3) {
                target = org.bukkit.Bukkit.getPlayerExact(args[2]);
            } else if (sender instanceof org.bukkit.entity.Player p) {
                target = p;
            }

            if (target == null) {
                sender.sendMessage("§cИгрок не найден.");
                return true;
            }

            plugin.getMenus().openMenu(target, menuId, 0);
            sender.sendMessage("§aОткрыто меню: " + menuId + " для " + target.getName());
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            if (plugin.getMenus() != null) {
                plugin.getMenus().reload();
            }

            sender.sendMessage("§a[LogicMenu] Конфиг перезагружен");
            return true;
        }

        sender.sendMessage("§cИспользование: /pbg reload");
        sender.sendMessage("§cИспользование: /pbg open <menu|alias> [player]");
        return true;
    }
}

