// console command to accounts managment
package ru.rexlite.auth.commands;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;
import ru.rexlite.auth.MainAuth;

public class AuthCommand extends Command {

    private final MainAuth plugin;

    public AuthCommand(MainAuth plugin) {
        super("auth", "Console command to manage authentication system", "/auth <info|reset|reload> [player]");
        this.plugin = plugin;
        this.setPermission("auth.admin");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("auth.admin")) {
            sender.sendMessage(TextFormat.RED + "%commands.generic.permission");
            return false;
        }

        if (!(sender instanceof cn.nukkit.command.ConsoleCommandSender)) {
            sender.sendMessage(TextFormat.RED + "This command can only be used from the console!");
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(TextFormat.YELLOW + "Usage: /auth <info|reset|reload> [player]");
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                if (args.length != 2) {
                    sender.sendMessage(TextFormat.RED + "Usage: /auth info <player>");
                    return false;
                }
                String nick = args[1].toLowerCase();
                if (!plugin.getDataProvider().isRegistered(nick)) {
                    sender.sendMessage(TextFormat.RED + "Player " + nick + " is not registered!");
                    return false;
                }
                String passwordHash = plugin.getDataProvider().getPasswordHash(nick);
                sender.sendMessage(TextFormat.GREEN + "Player: " + nick + "\nPassword Hash: " + passwordHash);
                return true;

            case "reset":
                if (args.length != 2) {
                    sender.sendMessage(TextFormat.RED + "Usage: /auth reset <player>");
                    return false;
                }
                nick = args[1].toLowerCase();
                if (!plugin.getDataProvider().isRegistered(nick)) {
                    sender.sendMessage(TextFormat.RED + "Player " + nick + " is not registered!");
                    return false;
                }
                plugin.getAuthManager().resetPassword(nick);
                sender.sendMessage(TextFormat.GREEN + "Password for player " + nick + " has been reset.");
                return true;

            case "reload":
                plugin.getConfigManager().initConfigs();
                sender.sendMessage(TextFormat.GREEN + "Auth plugin configuration reloaded.");
                return true;

            default:
                sender.sendMessage(TextFormat.YELLOW + "Usage: /auth <info|reset|reload> [player]");
                return false;
        }
    }
}
