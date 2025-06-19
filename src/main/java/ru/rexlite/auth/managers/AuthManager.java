package ru.rexlite.auth.managers;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.network.protocol.CommandRequestPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.utils.TextFormat;
import ru.rexlite.auth.MainAuth;
import ru.rexlite.auth.providers.DataProvider;

import java.security.MessageDigest;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class AuthManager implements Listener {

    private final MainAuth plugin;
    private final ConfigManager configManager;
    private final DataProvider dataProvider;
    private final Map<String, Boolean> isAuth = new ConcurrentHashMap<>();
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, Integer> timeoutTasks = new ConcurrentHashMap<>();
    private final Map<String, String> restoreCodes = new ConcurrentHashMap<>();

    public AuthManager(MainAuth plugin, ConfigManager configManager, DataProvider dataProvider) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataProvider = dataProvider;
    }

    public static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String generateRestoreCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    public String generateNewPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    public void cancelTimeout(String nick) {
        if (timeoutTasks.containsKey(nick)) {
            plugin.getServer().getScheduler().cancelTask(timeoutTasks.remove(nick));
        }
    }

    public void startTimeout(String nick) {
        int seconds = configManager.getConfig().getInt("form-timeout-seconds", 120);
        int taskId = plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
            Player p = plugin.getServer().getPlayerExact(nick);
            if (p != null && p.isOnline() && !isAuth.getOrDefault(nick, false)) {
                p.kick(TextFormat.colorize(configManager.getMessages().getString("timeout-kick", "You took too long to authenticate!")), false);
            }
        }, 20 * seconds).getTaskId();
        timeoutTasks.put(nick, taskId);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String nick = player.getName().toLowerCase();
        isAuth.put(nick, false);

        if (configManager.getConfig().getBoolean("isOffGamemodeOrFly")) {
            player.setAllowFlight(false);
            player.setGamemode(0);
        }

        if (configManager.getConfig().getBoolean("isAttempts")) {
            attempts.put(nick, 1);
        }

        if (!dataProvider.isRegistered(nick)) {
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("join-message-register")));
            if (configManager.getAccounts().getString(player.getClientId().toString()).isEmpty()) {
                configManager.getAccounts().set(player.getClientId().toString(), 0);
                configManager.getAccounts().save();
            }
            plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
                if (!isAuth.getOrDefault(nick, false)) {
                    plugin.getFormManager().showRegisterForm(player);
                    startTimeout(nick);
                }
            }, 20);
        } else {
            String storedClientId = dataProvider.getClientId(nick);
            String currentClientId = player.getClientId().toString();
            if (storedClientId != null && storedClientId.equals(currentClientId) && configManager.getConfig().getBoolean("ip-players-save")) {
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("log-in")));
                isAuth.put(nick, true);
            } else {
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("join-message-login")));
                plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
                    if (!isAuth.getOrDefault(nick, false)) {
                        plugin.getFormManager().showLoginForm(player);
                        startTimeout(nick);
                    }
                }, 20);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String nick = player.getName().toLowerCase();
        if (!isAuth.getOrDefault(nick, false)) {
            event.setCancelled();
            String pass = event.getMessage().split(" ")[0];
            handlePasswordInput(player, pass);
        }
    }

    public void handlePasswordInput(Player player, String pass) {
        String nick = player.getName().toLowerCase();
        int min = configManager.getConfig().getInt("min_count_symbols");
        int max = configManager.getConfig().getInt("max_count_symbols");

        if (!dataProvider.isRegistered(nick)) {
            // Registration by chat for old MCBE versions
            if (pass.length() < min) {
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("short-password")));
            } else if (pass.length() > max) {
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("long-password")));
            } else {
                String[] weakPasswords = configManager.getConfig().getString("list-easy-password").split(",");
                for (String weak : weakPasswords) {
                    if (pass.equalsIgnoreCase(weak.trim()) && !weak.trim().isEmpty()) {
                        player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("easy-password")));
                        return;
                    }
                }
                if (configManager.getAccounts().getInt(player.getClientId().toString()) < configManager.getConfig().getInt("isCountAccounts")) {
                    // Check IP account limit
                    if (configManager.getConfig().getBoolean("enable-max-accounts-per-ip")) {
                        int maxAccountsPerIp = configManager.getConfig().getInt("max-accounts-per-ip");
                        int accountsByIp = dataProvider.getAccountsByIp(player.getAddress());
                        if (accountsByIp >= maxAccountsPerIp) {
                            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("too-many-accounts-ip")));
                            player.kick(TextFormat.colorize(configManager.getMessages().getString("too-many-accounts-ip")), false);
                            return;
                        }
                    }
                    configManager.getAccounts().set(player.getClientId().toString(), configManager.getAccounts().getInt(player.getClientId().toString()) + 1);
                    configManager.getAccounts().save();
                    dataProvider.register(nick, sha256(pass), player.getAddress(), player.getClientId().toString(), "");
                    isAuth.put(nick, true);
                    cancelTimeout(nick);
                    player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("reg-message")));
                    if (configManager.getConfig().getBoolean("isNewPlayerMessage")) {
                        configManager.getConfig().set("count_reg_players", configManager.getConfig().getInt("count_reg_players") + 1);
                        configManager.getConfig().save();
                        plugin.getServer().broadcastMessage(TextFormat.colorize(configManager.getMessages().getString("new-player")
                                .replace("{nick}", nick).replace("{number}", String.valueOf(configManager.getConfig().getInt("count_reg_players")))));
                    }
                } else {
                    player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("create_many_accounts")));
                }
            }
        } else {
            // Login by chat for old MCBE versions
            String passHash = sha256(pass);
            if (dataProvider.isPasswordCorrect(nick, passHash)) {
                dataProvider.updateClientId(nick, player.getClientId().toString());
                isAuth.put(nick, true);
                cancelTimeout(nick);
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("log-in")));
            } else {
                int current = attempts.getOrDefault(nick, 1);
                if (current >= configManager.getConfig().getInt("count-attempts", 3)) {
                    player.kick(TextFormat.colorize(configManager.getMessages().getString("incorrect-password")), false);
                } else {
                    attempts.put(nick, current + 1);
                    player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("incorrect-password")));
                    player.sendActionBar(TextFormat.colorize(configManager.getMessages().getString("incorrect-password")));
                    plugin.getFormManager().showForgotPasswordForm(player);
                }
            }
        }
    }

    public void handleChangePassword(Player player, String oldPass, String newPass, String repeatPass) {
        String nick = player.getName().toLowerCase();
        int min = configManager.getConfig().getInt("min_count_symbols");
        int max = configManager.getConfig().getInt("max_count_symbols");

        if (!dataProvider.isPasswordCorrect(nick, sha256(oldPass))) {
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("changepassword-incorrect-password")));
            return;
        }
        if (!newPass.equals(repeatPass)) {
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("passwords-not-match")));
            return;
        }
        if (newPass.length() < min) {
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("short-new-password")));
            return;
        }
        if (newPass.length() > max) {
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("long-new-password")));
            return;
        }
        String[] weakPasswords = configManager.getConfig().getString("list-easy-password").split(",");
        for (String weak : weakPasswords) {
            if (newPass.equalsIgnoreCase(weak.trim()) && !weak.trim().isEmpty()) {
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("easy-new-password")));
                return;
            }
        }

        dataProvider.updatePassword(nick, sha256(newPass));
        player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("reg-changepassword-message").replace("{password}", newPass)));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isAuth.getOrDefault(event.getPlayer().getName().toLowerCase(), false)) {
            event.setCancelled();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isAuth.getOrDefault(player.getName().toLowerCase(), false)) {
            event.setCancelled();
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("no-login")));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isAuth.getOrDefault(player.getName().toLowerCase(), false)) {
            event.setCancelled();
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("no-login")));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommandPacket(DataPacketReceiveEvent event) {
        DataPacket packet = event.getPacket();
        if (packet instanceof CommandRequestPacket cmdPacket) {
            Player player = event.getPlayer();
            String nick = player.getName().toLowerCase();
            if (!isAuth.getOrDefault(nick, false) && !cmdPacket.command.toLowerCase().startsWith("changepassword")) {
                event.setCancelled(true);
                player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("no-login")));
            }
        }
    }

    public boolean isAuthenticated(String nick) {
        return isAuth.getOrDefault(nick.toLowerCase(), false);
    }

    public void setAuthenticated(String nick, boolean authenticated) {
        isAuth.put(nick.toLowerCase(), authenticated);
    }

    public Map<String, String> getRestoreCodes() {
        return restoreCodes;
    }

    public void resetPassword(String nick) {
        String newPass = generateNewPassword();
        dataProvider.updatePassword(nick, sha256(newPass));
        Player player = plugin.getServer().getPlayerExact(nick);
        if (player != null && player.isOnline()) {
            player.sendMessage(TextFormat.colorize(configManager.getMessages().getString("reset-password-message").replace("{password}", newPass)));
        }
    }
}
