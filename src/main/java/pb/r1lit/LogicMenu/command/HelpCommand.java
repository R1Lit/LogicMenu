package pb.r1lit.LogicMenu.command;

import org.bukkit.command.CommandSender;

public class HelpCommand implements LmSubCommand {
    private final CommandRouter router;

    public HelpCommand(CommandRouter router) {
        this.router = router;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "Show all subcommands";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("§eLogicMenu commands:");
        for (LmSubCommand cmd : router.uniqueCommands()) {
            String line = "§6/logicmenu " + cmd.name();
            if (!cmd.usage().isBlank()) {
                line += " §7" + cmd.usage();
            }
            if (!cmd.description().isBlank()) {
                line += " §f- " + cmd.description();
            }
            sender.sendMessage(line);
        }
        return true;
    }
}
