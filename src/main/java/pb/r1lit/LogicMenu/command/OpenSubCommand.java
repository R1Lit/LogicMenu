package pb.r1lit.LogicMenu.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pb.r1lit.LogicMenu.LogicMenu;

import java.util.ArrayList;
import java.util.List;

public class OpenSubCommand implements LmSubCommand {
    private final LogicMenu plugin;

    public OpenSubCommand(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "open";
    }

    @Override
    public String permission() {
        return "logicmenu.open";
    }

    @Override
    public String description() {
        return "Open a menu for a player";
    }

    @Override
    public String usage() {
        return "<menu|alias> [player] [page]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /logicmenu open <menu|alias> [player] [page]");
            return true;
        }
        if (plugin.getMenus() == null) {
            sender.sendMessage("§cMenu engine is not loaded.");
            return true;
        }

        String menuId = plugin.getMenus().resolveMenuId(args[0]);
        Player target = null;
        int page = 0;

        if (args.length >= 2) {
            Player p = Bukkit.getPlayerExact(args[1]);
            if (p != null) {
                target = p;
            } else if (sender instanceof Player sp) {
                target = sp;
            }
        } else if (sender instanceof Player sp) {
            target = sp;
        }

        if (args.length >= 3) {
            try {
                page = Math.max(0, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
            }
        }

        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        plugin.getMenus().openMenu(target, menuId, page);
        sender.sendMessage("§aOpened menu: " + menuId + " for " + target.getName());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (plugin.getMenus() == null) return out;
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String id : plugin.getMenus().getMenuIds()) {
                if (id.toLowerCase().startsWith(prefix)) out.add(id);
            }
        } else if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) out.add(p.getName());
            }
        }
        return out;
    }
}
