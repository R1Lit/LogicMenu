package pb.r1lit.LogicMenu.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import pb.r1lit.LogicMenu.LogicMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandRouter implements CommandExecutor, TabCompleter {
    private final LogicMenu plugin;
    private final Map<String, LmSubCommand> commands = new HashMap<>();

    public CommandRouter(LogicMenu plugin) {
        this.plugin = plugin;
        register(new HelpCommand(this));
        register(new ReloadSubCommand(plugin));
        register(new OpenSubCommand(plugin));
        register(new ListSubCommand(plugin));
        register(new DumpSubCommand(plugin));
        register(new ExecuteSubCommand(plugin));
        register(new ValidateSubCommand(plugin));
        register(new ExpansionsSubCommand(plugin));
        register(new MetaSubCommand(plugin));
    }

    private void register(LmSubCommand cmd) {
        commands.put(cmd.name().toLowerCase(Locale.ROOT), cmd);
        for (String alias : cmd.aliases()) {
            commands.put(alias.toLowerCase(Locale.ROOT), cmd);
        }
    }

    public List<LmSubCommand> uniqueCommands() {
        List<LmSubCommand> list = new ArrayList<>();
        for (LmSubCommand cmd : commands.values()) {
            if (!list.contains(cmd)) list.add(cmd);
        }
        list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return list;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("logicmenu.use") && !sender.hasPermission("logicmenu.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            return commands.get("help").execute(sender, new String[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        LmSubCommand cmd = commands.get(sub);
        if (cmd == null) {
            sender.sendMessage("§cUnknown subcommand. Use /" + label + " help.");
            return true;
        }
        String perm = cmd.permission();
        if (perm != null && !perm.isBlank() && !sender.hasPermission(perm)) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return cmd.execute(sender, rest);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("logicmenu.use") && !sender.hasPermission("logicmenu.admin")) {
            return List.of();
        }
        if (args.length <= 1) {
            List<String> out = new ArrayList<>();
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            for (LmSubCommand cmd : uniqueCommands()) {
                String name = cmd.name();
                if (!prefix.isBlank() && !name.startsWith(prefix)) continue;
                String perm = cmd.permission();
                if (perm != null && !perm.isBlank() && !sender.hasPermission(perm)) continue;
                out.add(name);
            }
            return out;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        LmSubCommand cmd = commands.get(sub);
        if (cmd == null) return List.of();
        String perm = cmd.permission();
        if (perm != null && !perm.isBlank() && !sender.hasPermission(perm)) return List.of();
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return cmd.tabComplete(sender, rest);
    }
}
