package pb.r1lit.LogicMenu.command;

import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;

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
            sender.sendMessage(plugin.getLang().get("list.menu_engine_missing", "&cMenu engine is not loaded."));
            return true;
        }

        var ids = plugin.getMenus().getMenuIds();
        sender.sendMessage(plugin.getLang().get("list.loaded", "&eLoaded menus: &f{count}")
                .replace("{count}", String.valueOf(ids.size())));
        if (!ids.isEmpty()) {
            sender.sendMessage(plugin.getLang().get("list.items", "&7{list}")
                    .replace("{list}", String.join(", ", ids)));
        }
        return true;
    }
}
