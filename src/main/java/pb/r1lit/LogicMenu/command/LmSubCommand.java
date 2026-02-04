package pb.r1lit.LogicMenu.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface LmSubCommand {
    String name();

    default List<String> aliases() {
        return List.of();
    }

    default String permission() {
        return "logicmenu.admin";
    }

    default String description() {
        return "";
    }

    default String usage() {
        return "";
    }

    boolean execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
