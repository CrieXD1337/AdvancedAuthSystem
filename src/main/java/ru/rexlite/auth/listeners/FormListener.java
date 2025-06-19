package ru.rexlite.auth.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.utils.TextFormat;
import ru.rexlite.auth.MainAuth;
import ru.rexlite.auth.managers.AuthManager;
import ru.rexlite.auth.managers.ConfigManager;
import ru.rexlite.auth.providers.DataProvider;

public class FormListener implements Listener {

    private final MainAuth plugin;
    private final AuthManager authManager;
    private final ConfigManager configManager;
    private final DataProvider dataProvider;

    public FormListener(MainAuth plugin, AuthManager authManager, ConfigManager configManager, DataProvider dataProvider) {
        this.plugin = plugin;
        this.authManager = authManager;
        this.configManager = configManager;
        this.dataProvider = dataProvider;
    }

    @EventHandler
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        FormWindow window = event.getWindow();
        int formId = event.getFormID();
        String nick = player.getName().toLowerCase();

        if (event.wasClosed()) {
            if (!authManager.isAuthenticated(nick)) {
                authManager.cancelTimeout(nick);
                plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
                    if (player.isOnline() && !authManager.isAuthenticated(nick)) {
                        switch (formId) {
                            case 1009 -> plugin.getFormManager().showLoginForm(player);
                            case 1010 -> plugin.getFormManager().showForgotPasswordForm(player);
                            case 1011 -> plugin.getFormManager().showRegisterForm(player);
                            case 1013 -> plugin.getFormManager().showChangePasswordForm(player);
                        }
                        authManager.startTimeout(nick);
                    }
                }, 2);
            }
            return;
        }

        switch (formId) {
            case 1009 -> handleLoginForm(player, window);
            case 1010 -> handleForgotPasswordForm(player, window);
            case 1011 -> handleRegisterForm(player, window);
            case 1013 -> handleChangePasswordForm(player, window);
        }
    }

    private void handleLoginForm(Player player, FormWindow window) {
        String nick = player.getName().toLowerCase();
        FormResponseCustom response = (FormResponseCustom) window.getResponse();
        String input = response.getInputResponse(1);
        if (input != null && !input.trim().isEmpty()) {
            authManager.handlePasswordInput(player, input);
        } else {
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("password-empty")));
            player.sendActionBar(TextFormat.colorize(configManager.getMessages().getString("password-empty")));
            plugin.getFormManager().showLoginForm(player);
        }
    }

    private void handleForgotPasswordForm(Player player, FormWindow window) {
        FormResponseSimple response = (FormResponseSimple) window.getResponse();
        String buttonText = response.getClickedButton().getText();
        if (buttonText.equals(configManager.getForms().getString("forgot-password.button-try-again"))) {
            plugin.getFormManager().showLoginForm(player);
        } else if (buttonText.equals(configManager.getForms().getString("forgot-password.button-leave"))) {
            player.kick(configManager.getMessages().getString("kick-reason"), false);
        }
    }

    private void handleRegisterForm(Player player, FormWindow window) {
        String nick = player.getName().toLowerCase();
        FormResponseCustom response = (FormResponseCustom) window.getResponse();
        String pass = response.getInputResponse(1);

        if (!dataProvider.isRegistered(nick)) {
            int min = configManager.getConfig().getInt("min_count_symbols");
            int max = configManager.getConfig().getInt("max_count_symbols");

            if (pass == null || pass.length() < min) {
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("short-password")));
                plugin.getFormManager().showRegisterForm(player);
                return;
            }

            if (pass.length() > max) {
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("long-password")));
                plugin.getFormManager().showRegisterForm(player);
                return;
            }

            String[] weakPasswords = configManager.getConfig().getString("list-easy-password").split(",");
            for (String weak : weakPasswords) {
                if (pass.equalsIgnoreCase(weak.trim()) && !weak.trim().isEmpty()) {
                    player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("easy-password")));
                    plugin.getFormManager().showRegisterForm(player);
                    return;
                }
            }

            if (configManager.getAccounts().getInt(player.getClientId().toString()) < configManager.getConfig().getInt("isCountAccounts")) {
                configManager.getAccounts().set(player.getClientId().toString(), configManager.getAccounts().getInt(player.getClientId().toString()) + 1);
                configManager.getAccounts().save();
                dataProvider.register(nick, AuthManager.sha256(pass), player.getAddress(), player.getClientId().toString(), "");
                authManager.setAuthenticated(nick, true);
                authManager.cancelTimeout(nick);
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("reg-message").replace("{password}", pass)));
                if (configManager.getConfig().getBoolean("isNewPlayerMessage")) {
                    configManager.getConfig().set("count_reg_players", configManager.getConfig().getInt("count_reg_players") + 1);
                    configManager.getConfig().save();
                    plugin.getServer().broadcastMessage(TextFormat.colorize(configManager.getMessages().getString("new-player")
                            .replace("{nick}", nick).replace("{number}", String.valueOf(configManager.getConfig().getInt("count_reg_players")))));
                }
            } else {
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("create_many_accounts")));
            }
        } else {
            player.sendMessage(TextFormat.RED + "Error! Please login!");
            plugin.getFormManager().showLoginForm(player);
        }
    }

    private void handleChangePasswordForm(Player player, FormWindow window) {
        String nick = player.getName().toLowerCase();
        FormResponseCustom response = (FormResponseCustom) window.getResponse();

        if (response == null) {
            player.sendMessage(TextFormat.RED + "Error: The form was not filled out.");
            plugin.getFormManager().showChangePasswordForm(player);
            return;
        }

        String oldPass = response.getInputResponse(1);
        String newPass = response.getInputResponse(2);
        String repeatPass = response.getInputResponse(3);

        if (oldPass == null || newPass == null || repeatPass == null || oldPass.trim().isEmpty() || newPass.trim().isEmpty() || repeatPass.trim().isEmpty()) {
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("fields-required")));
            plugin.getFormManager().showChangePasswordForm(player);
            return;
        }

        authManager.handleChangePassword(player, oldPass, newPass, repeatPass);
    }
}