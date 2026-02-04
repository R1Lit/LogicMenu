package pb.r1lit.LogicMenu.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.gui.model.MenuAction;

import java.util.ArrayList;
import java.util.List;

public class ExecuteSubCommand implements LmSubCommand {
    private final LogicMenu plugin;

    public ExecuteSubCommand(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "execute";
    }

    @Override
    public String permission() {
        return "logicmenu.execute";
    }

    @Override
    public String description() {
        return "Execute one action string";
    }

    @Override
    public String usage() {
        return "<action> [player]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /logicmenu execute <action> [player]");
            return true;
        }
        if (plugin.getMenus() == null) {
            sender.sendMessage("§cMenu engine is not loaded.");
            return true;
        }

        String actionRaw;
        Player target = null;
        if (args.length >= 2) {
            Player p = Bukkit.getPlayerExact(args[args.length - 1]);
            if (p != null) {
                target = p;
                actionRaw = joinArgs(args, 0, args.length - 1);
            } else {
                actionRaw = joinArgs(args, 0, args.length);
            }
        } else {
            actionRaw = args[0];
        }

        if (target == null) {
            if (sender instanceof Player sp) {
                target = sp;
            } else {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
        }

        MenuAction action = MenuAction.parse(actionRaw);
        plugin.getMenus().executeAction(target, action, plugin.getMenus().createBaseVars(target));
        sender.sendMessage("§aExecuted action for " + target.getName() + ": " + actionRaw);
        return true;
    }

    private String joinArgs(String[] args, int start, int end) {
        List<String> parts = new ArrayList<>();
        for (int i = start; i < end; i++) {
            parts.add(args[i]);
        }
        return String.join(" ", parts);
    }
}
