package pb.r1lit.LogicMenu.command.Sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pb.r1lit.LogicMenu.LogicMenu;

public class Execute implements Lm {
    private final LogicMenu plugin;

    public Execute(LogicMenu plugin) {
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
        return "Execute raw action";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getLang().get("execute.usage", "&cUsage: /logicmenu execute <action> [player]"));
            return true;
        }
        if (plugin.getMenus() == null) {
            sender.sendMessage(plugin.getLang().get("execute.menu_engine_missing", "&cMenu engine is not loaded."));
            return true;
        }

        Player target = null;
        String actionRaw;
        if (args.length > 1) {
            Player last = Bukkit.getPlayerExact(args[args.length - 1]);
            if (last != null) {
                target = last;
                actionRaw = String.join(" ", java.util.Arrays.copyOf(args, args.length - 1));
            } else {
                actionRaw = String.join(" ", args);
            }
        } else {
            actionRaw = args[0];
        }

        if (target == null && sender instanceof Player player) {
            target = player;
        }
        if (target == null) {
            sender.sendMessage(plugin.getLang().get("execute.player_not_found", "&cPlayer not found."));
            return true;
        }

        var action = pb.r1lit.LogicMenu.gui.model.MenuAction.parse(actionRaw);

        plugin.getMenus().executeAction(target, action, java.util.Map.of());
        sender.sendMessage(plugin.getLang().get("execute.executed", "&aExecuted action for {player}: {action}")
                .replace("{player}", target.getName())
                .replace("{action}", actionRaw));
        return true;
    }
}
