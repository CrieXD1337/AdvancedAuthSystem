package ru.rexlite.auth.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import ru.rexlite.auth.MainAuth;

import java.util.List;

public class ChangePassword extends Command {

    private final MainAuth plugin;

    public ChangePassword(MainAuth plugin) {
        super(getCommandName(plugin), getCommandDescription(plugin), null, getCommandAliases(plugin));
        this.plugin = plugin;
        setPermission(getCommandPermission(plugin));
    }

    private static String getCommandName(MainAuth plugin) {
        Config config = plugin.getConfigManager().getConfig();
        return config.getString("commands.changepassword.name", "changepassword");
    }

    private static String getCommandDescription(MainAuth plugin) {
        Config config = plugin.getConfigManager().getConfig();
        return config.getString("commands.changepassword.description", "Change your password");
    }

    private static String[] getCommandAliases(MainAuth plugin) {
        Config config = plugin.getConfigManager().getConfig();
        List<String> aliases = config.getStringList("commands.changepassword.aliases");
        return aliases.toArray(new String[0]);
    }

    private static String getCommandPermission(MainAuth plugin) {
        Config config = plugin.getConfigManager().getConfig();
        return config.getString("commands.changepassword.permission", "auth.commands.changepassword");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextFormat.colorize("&cOnly for players!"));
            return false;
        }

        Player player = (Player) sender;
        String nick = player.getName().toLowerCase();

        if (!plugin.getAuthManager().isAuthenticated(nick)) {
            player.sendMessage(TextFormat.colorize(plugin.getConfigManager().getMessages().getString("no-login")));
            return false;
        }

        if (args.length == 0) {
            plugin.getFormManager().showChangePasswordForm(player);
        } else if (args.length == 3) {
            String oldPass = args[0];
            String newPass = args[1];
            String repeatPass = args[2];
            plugin.getAuthManager().handleChangePassword(player, oldPass, newPass, repeatPass);
        } else {
            player.sendMessage(TextFormat.colorize(plugin.getConfigManager().getMessages().getString("changepassword-help")));
        }

        return true;
    }
}