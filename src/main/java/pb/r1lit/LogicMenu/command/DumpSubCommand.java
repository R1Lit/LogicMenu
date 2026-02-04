package pb.r1lit.LogicMenu.command;

import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;

public class DumpSubCommand implements LmSubCommand {
    private final LogicMenu plugin;

    public DumpSubCommand(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "dump";
    }

    @Override
    public String permission() {
        return "logicmenu.dump";
    }

    @Override
    public String description() {
        return "Dump menu info";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getLang().get("dump.usage", "&cUsage: /logicmenu dump <menu|alias>"));
            return true;
        }

        if (plugin.getMenus() == null) {
            sender.sendMessage(plugin.getLang().get("dump.menu_engine_missing", "&cMenu engine is not loaded."));
            return true;
        }

        String menuId = args[0];
        var def = plugin.getMenus().getMenuDefinition(menuId);
        if (def == null) {
            sender.sendMessage(plugin.getLang().get("dump.menu_not_found", "&cMenu not found: {menu}")
                    .replace("{menu}", menuId));
            return true;
        }

        sender.sendMessage(plugin.getLang().get("dump.menu", "&eMenu: &f{menu}")
                .replace("{menu}", def.getId()));
        sender.sendMessage(plugin.getLang().get("dump.title", "&7Title: &f{title}")
                .replace("{title}", def.getTitle()));
        sender.sendMessage(plugin.getLang().get("dump.size", "&7Size: &f{size}")
                .replace("{size}", String.valueOf(def.getSize())));
        sender.sendMessage(plugin.getLang().get("dump.items", "&7Items: &f{count}")
                .replace("{count}", String.valueOf(def.getItems() != null ? def.getItems().size() : 0)));
        sender.sendMessage(plugin.getLang().get("dump.fill", "&7Fill item: &f{value}")
                .replace("{value}", String.valueOf(def.getFillItem() != null)));
        sender.sendMessage(plugin.getLang().get("dump.open_commands", "&7Open commands: &f{count}")
                .replace("{count}", String.valueOf(def.getOpenCommands() != null ? def.getOpenCommands().size() : 0)));
        sender.sendMessage(plugin.getLang().get("dump.dynamic", "&7Dynamic: &f{value}")
                .replace("{value}", String.valueOf(def.getDynamic() != null)));
        sender.sendMessage(plugin.getLang().get("dump.dynamics", "&7Dynamics: &f{count}")
                .replace("{count}", String.valueOf(def.getDynamics() != null ? def.getDynamics().size() : 0)));
        sender.sendMessage(plugin.getLang().get("dump.permission", "&7Permission: &f{perm}")
                .replace("{perm}", def.getPermission() == null ? "" : def.getPermission()));
        sender.sendMessage(plugin.getLang().get("dump.update", "&7Update: &f{value} ({ticks} ticks)")
                .replace("{value}", String.valueOf(def.isUpdate()))
                .replace("{ticks}", String.valueOf(def.getUpdateIntervalTicks())));
        return true;
    }
}
