package pb.r1lit.LogicMenu.command;

import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.gui.model.MenuDefinition;

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
        return "Show menu meta info";
    }

    @Override
    public String usage() {
        return "<menu|alias>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /logicmenu dump <menu|alias>");
            return true;
        }
        if (plugin.getMenus() == null) {
            sender.sendMessage("§cMenu engine is not loaded.");
            return true;
        }

        String menuId = plugin.getMenus().resolveMenuId(args[0]);
        MenuDefinition def = plugin.getMenus().getMenuDefinition(menuId);
        if (def == null) {
            sender.sendMessage("§cMenu not found: " + menuId);
            return true;
        }

        sender.sendMessage("§eMenu: §f" + def.getId());
        sender.sendMessage("§7Title: §f" + def.getTitle());
        sender.sendMessage("§7Size: §f" + def.getSize());
        sender.sendMessage("§7Items: §f" + (def.getItems() != null ? def.getItems().size() : 0));
        sender.sendMessage("§7Fill item: §f" + (def.getFillItem() != null));
        sender.sendMessage("§7Open commands: §f" + (def.getOpenCommands() != null ? def.getOpenCommands().size() : 0));
        sender.sendMessage("§7Dynamic: §f" + (def.getDynamic() != null));
        sender.sendMessage("§7Dynamics: §f" + (def.getDynamics() != null ? def.getDynamics().size() : 0));
        sender.sendMessage("§7Permission: §f" + (def.getPermission() == null ? "" : def.getPermission()));
        sender.sendMessage("§7Update: §f" + def.isUpdate() + " (" + def.getUpdateIntervalTicks() + " ticks)");
        return true;
    }
}
