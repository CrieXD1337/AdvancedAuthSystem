package ru.rexlite.auth;

import cn.nukkit.event.Listener;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import ru.rexlite.auth.commands.ChangePassword;
import ru.rexlite.auth.listeners.FormListener;
import ru.rexlite.auth.managers.*;
import ru.rexlite.auth.providers.DataProvider;
import ru.rexlite.auth.providers.MySqlDataProvider;
import ru.rexlite.auth.providers.YamlDataProvider;

public class MainAuth extends PluginBase implements Listener {

    private ConfigManager configManager;
    private DataProvider dataProvider;
    private AuthManager authManager;
    private FormManager formManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.initConfigs();

        String provider = configManager.getConfig().getString("provider", "YAML");
        if (provider.equalsIgnoreCase("MySQL")) {
            this.dataProvider = new MySqlDataProvider(this, configManager);
        } else {
            this.dataProvider = new YamlDataProvider(this, configManager);
        }

        this.authManager = new AuthManager(this, configManager, dataProvider);
        this.formManager = new FormManager(configManager);

        this.getLogger().info("ASU - Auth plugin enabled");

        getServer().getPluginManager().registerEvents(authManager, this);
        getServer().getPluginManager().registerEvents(new FormListener(this, authManager, configManager, dataProvider), this);

        ChangePassword changePasswordCommand = new ChangePassword(this);
        getServer().getCommandMap().register(changePasswordCommand.getName(), changePasswordCommand);

        getDataFolder().mkdirs();
    }

    @Override
    public void onDisable() {

        this.getLogger().info(TextFormat.YELLOW + "ASU - Auth plugin disabled");

        if (dataProvider != null) {
            dataProvider.close();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public FormManager getFormManager() {
        return formManager;
    }
}