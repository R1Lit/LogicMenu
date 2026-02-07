package pb.r1lit.LogicMenu.command;

import org.bukkit.command.CommandSender;
import pb.r1lit.LogicMenu.command.Sub.Lm;

public class HelpCommand implements Lm {
    private final CommandRouter router;

    public HelpCommand(CommandRouter router) {
        this.router = router;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String permission() {
        return "logicmenu.help";
    }

    @Override
    public String description() {
        return "Show help";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        var plugin = router.getPlugin();
        sender.sendMessage(plugin.getLang().get("help.header", "&eLogicMenu commands:"));
        for (Lm sub : router.uniqueCommands()) {
            if (sub.permission() != null && !sub.permission().isBlank() && !sender.hasPermission(sub.permission())) continue;
            String line = plugin.getLang().get("help.line", "&7/{label} {name} {usage} &f- {desc}")
                    .replace("{label}", "logicmenu")
                    .replace("{name}", sub.name())
                    .replace("{usage}", sub.usage() == null ? "" : sub.usage())
                    .replace("{desc}", sub.description() == null ? "" : sub.description());
            sender.sendMessage(line);
        }
        return true;
    }
}
