package ru.rexlite.auth.managers;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.io.File;

public class ConfigManager {

    private final PluginBase plugin;
    private Config messages;
    private Config config;
    private Config accounts;
    private Config forms;
    private Config passwords;
    private Config ip;

    public ConfigManager(PluginBase plugin) {
        this.plugin = plugin;
    }

    public void initConfigs() {
        plugin.saveResource("messages.yml");
        plugin.saveResource("config.yml");
        plugin.saveResource("accounts.yml");
        plugin.saveResource("forms.yml");
        plugin.saveResource("passwords.yml");
        plugin.saveResource("ip.yml");

        this.messages = new Config(new File(plugin.getDataFolder(), "messages.yml"), Config.YAML);
        this.config = new Config(new File(plugin.getDataFolder(), "config.yml"), Config.YAML);
        this.accounts = new Config(new File(plugin.getDataFolder(), "accounts.yml"), Config.YAML);
        this.forms = new Config(new File(plugin.getDataFolder(), "forms.yml"), Config.YAML);
        this.passwords = new Config(new File(plugin.getDataFolder(), "passwords.yml"), Config.YAML);
        this.ip = new Config(new File(plugin.getDataFolder(), "ip.yml"), Config.YAML);
    }

    public Config getMessages() {
        return messages;
    }

    public Config getConfig() {
        return config;
    }

    public Config getAccounts() {
        return accounts;
    }

    public Config getForms() {
        return forms;
    }

    public Config getPasswords() {
        return passwords;
    }

    public Config getIp() {
        return ip;
    }
}