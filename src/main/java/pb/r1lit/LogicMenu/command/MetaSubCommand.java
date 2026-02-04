package pb.r1lit.LogicMenu.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;

import java.io.File;

public class MetaSubCommand implements LmSubCommand {
    private final LogicMenu plugin;

    public MetaSubCommand(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "meta";
    }

    @Override
    public String permission() {
        return "logicmenu.meta";
    }

    @Override
    public String description() {
        return "Show plugin meta info";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int menus = plugin.getMenus() != null ? plugin.getMenus().getMenuCount() : 0;
        File expansions = new File(plugin.getDataFolder(), "expansions");
        File[] jars = expansions.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        int expCount = jars == null ? 0 : jars.length;

        sender.sendMessage("§eLogicMenu §f" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Menus: §f" + menus);
        sender.sendMessage("§7Expansions: §f" + expCount);
        sender.sendMessage("§7GUI folder: §f" + new File(plugin.getDataFolder(), "gui").getPath());
        sender.sendMessage("§7Expansions folder: §f" + expansions.getPath());
        sender.sendMessage("§7PlaceholderAPI: §f" + (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "enabled" : "disabled"));
        return true;
    }
}
