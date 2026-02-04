package pb.r1lit.LogicMenu.command;

import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;

import java.util.List;

public class ListSubCommand implements LmSubCommand {
    private final LogicMenu plugin;

    public ListSubCommand(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String permission() {
        return "logicmenu.list";
    }

    @Override
    public String description() {
        return "List loaded menus";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (plugin.getMenus() == null) {
            sender.sendMessage("§cMenu engine is not loaded.");
            return true;
        }
        List<String> ids = plugin.getMenus().getMenuIds();
        ids.sort(String::compareToIgnoreCase);
        sender.sendMessage("§eLoaded menus: §f" + ids.size());
        if (!ids.isEmpty()) {
            sender.sendMessage("§7" + String.join(", ", ids));
        }
        return true;
    }
}
