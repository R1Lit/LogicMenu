package pb.r1lit.LogicMenu.command.Sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pb.r1lit.LogicMenu.LogicMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Meta implements Lm {
    private final LogicMenu plugin;

    public Meta(LogicMenu plugin) {
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
        return "Manage player meta values";
    }

    @Override
    public String usage() {
        return "<set|get|remove|toggle|increment> <player|me> <key> [value]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getLang().get("meta.usage",
                    "&cUsage: /logicmenu meta <set|get|remove|toggle|increment> <player|me> <key> [value]"));
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        String playerName = args[1];
        String key = args[2];
        Player target = resolvePlayer(sender, playerName);
        if (target == null) {
            sender.sendMessage(plugin.getLang().get("meta.player_not_found", "&cPlayer not found."));
            return true;
        }
        if (key == null || key.isBlank()) {
            sender.sendMessage(plugin.getLang().get("meta.usage",
                    "&cUsage: /logicmenu meta <set|get|remove|toggle|increment> <player|me> <key> [value]"));
            return true;
        }

        switch (action) {
            case "set" -> {
                String value = args.length > 3 ? join(args, 3) : "";
                plugin.getMetaStore().set(target, key, value);
                sender.sendMessage(plugin.getLang().get("meta.set", "&aMeta set: {player} {key} = {value}")
                        .replace("{player}", target.getName())
                        .replace("{key}", key)
                        .replace("{value}", value));
            }
            case "get" -> {
                String value = plugin.getMetaStore().get(target, key);
                sender.sendMessage(plugin.getLang().get("meta.get", "&eMeta: {player} {key} = {value}")
                        .replace("{player}", target.getName())
                        .replace("{key}", key)
                        .replace("{value}", value));
            }
            case "remove", "delete" -> {
                plugin.getMetaStore().remove(target, key);
                sender.sendMessage(plugin.getLang().get("meta.remove", "&aMeta removed: {player} {key}")
                        .replace("{player}", target.getName())
                        .replace("{key}", key));
            }
            case "toggle" -> {
                plugin.getMetaStore().toggle(target, key);
                String value = plugin.getMetaStore().get(target, key);
                sender.sendMessage(plugin.getLang().get("meta.toggle", "&aMeta toggled: {player} {key} = {value}")
                        .replace("{player}", target.getName())
                        .replace("{key}", key)
                        .replace("{value}", value));
            }
            case "increment", "inc", "add" -> {
                int delta = 1;
                if (args.length > 3) {
                    try {
                        delta = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(plugin.getLang().get("meta.invalid_delta", "&cInvalid number: {value}")
                                .replace("{value}", args[3]));
                        return true;
                    }
                }
                plugin.getMetaStore().increment(target, key, delta);
                String value = plugin.getMetaStore().get(target, key);
                sender.sendMessage(plugin.getLang().get("meta.increment", "&aMeta incremented: {player} {key} = {value}")
                        .replace("{player}", target.getName())
                        .replace("{key}", key)
                        .replace("{value}", value));
            }
            default -> sender.sendMessage(plugin.getLang().get("meta.usage",
                    "&cUsage: /logicmenu meta <set|get|remove|toggle|increment> <player|me> <key> [value]"));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("set", "get", "remove", "toggle", "increment");
        }
        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            if (sender instanceof Player) list.add("me");
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                list.add(player.getName());
            }
            return list;
        }
        return List.of();
    }

    private Player resolvePlayer(CommandSender sender, String name) {
        if (name == null) return null;
        if ("me".equalsIgnoreCase(name) && sender instanceof Player player) {
            return player;
        }
        return plugin.getServer().getPlayerExact(name);
    }

    private String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
