package pb.r1lit.LogicMenu.command.Sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.command.LmSubCommand;
import pb.r1lit.LogicMenu.command.util.PluginJarUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Expansions implements LmSubCommand {
    private final LogicMenu plugin;

    public Expansions(LogicMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "expansions";
    }

    @Override
    public String permission() {
        return "logicmenu.expansions";
    }

    @Override
    public String description() {
        return "Manage expansions";
    }

    @Override
    public String usage() {
        return "[list|reload|path]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "list";
        File folder = new File(plugin.getDataFolder(), "expansions");

        switch (sub) {
            case "path":
                sender.sendMessage(plugin.getLang().get("expansions.path", "&eExpansions folder: &f{path}")
                        .replace("{path}", folder.getPath()));
                return true;
            case "reload":
                int loaded = plugin.reloadExpansions();
                sender.sendMessage(plugin.getLang().get("expansions.reload", "&aExpansion scan complete. Loaded: &f{count}")
                        .replace("{count}", String.valueOf(loaded)));
                return true;
            case "list":
            default:
                File[] jars = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                if (jars == null || jars.length == 0) {
                    sender.sendMessage(plugin.getLang().get("expansions.none", "&eNo expansions found."));
                    return true;
                }
                sender.sendMessage(plugin.getLang().get("expansions.count", "&eExpansions: &f{count}")
                        .replace("{count}", String.valueOf(jars.length)));
                for (File jar : jars) {
                    Optional<String> name = PluginJarUtil.readPluginName(jar);
                    String pluginName = name.orElse(jar.getName());
                    boolean loadedFlag = Bukkit.getPluginManager().getPlugin(pluginName) != null;
                    sender.sendMessage(plugin.getLang().get("expansions.item", "&7- {name} &8({file}) &f{state}")
                            .replace("{name}", pluginName)
                            .replace("{file}", jar.getName())
                            .replace("{state}", loadedFlag ? "&aLOADED" : "&cNOT LOADED"));
                }
                return true;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String opt : List.of("list", "reload", "path")) {
                if (opt.startsWith(prefix)) out.add(opt);
            }
            return out;
        }
        return List.of();
    }
}
