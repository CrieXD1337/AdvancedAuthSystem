package ru.rexlite.auth.providers;

import cn.nukkit.utils.Config;
import ru.rexlite.auth.MainAuth;
import ru.rexlite.auth.managers.ConfigManager;

public class YamlDataProvider implements DataProvider {

    private final MainAuth plugin;
    private final ConfigManager configManager;
    private final Config passwords;
    private final Config ip;

    public YamlDataProvider(MainAuth plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.passwords = configManager.getPasswords();
        this.ip = configManager.getIp();
    }

    @Override
    public boolean isRegistered(String nick) {
        return passwords.exists(nick);
    }

    @Override
    public boolean isPasswordCorrect(String nick, String hash) {
        return hash.equals(passwords.getString(nick));
    }

    @Override
    public void register(String nick, String hash, String ip, String clientId, String email) {
        passwords.set(nick, hash);
        passwords.save();
        this.ip.set(nick, ip);
        this.ip.save();
    }

    @Override
    public void updatePassword(String nick, String newHash) {
        passwords.set(nick, newHash);
        passwords.save();
    }

    @Override
    public String getClientId(String nick) {
        return ip.getString(nick + ".clientId");
    }

    @Override
    public void updateClientId(String nick, String clientId) {
        ip.set(nick + ".clientId", clientId);
        ip.save();
    }

    @Override
    public int getAccountsByIp(String ip) {
        int count = 0;
        for (String key : this.ip.getKeys(false)) {
            if (this.ip.getString(key).equals(ip)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String getPasswordHash(String nick) {
        return passwords.getString(nick, null);
    }

    @Override
    public void close() {
        // don't need for yaml :)
    }
}
