package ru.rexlite.auth.managers;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;

public class FormManager {

    private final ConfigManager configManager;

    public FormManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void showLoginForm(Player player) {
        FormWindowCustom form = new FormWindowCustom(configManager.getForms().getString("login.title"));
        form.addElement(new ElementLabel(configManager.getForms().getString("login.label")));
        form.addElement(new ElementInput("", configManager.getForms().getString("login.input")));
        player.showFormWindow(form, 1009);
    }

    public void showRegisterForm(Player player) {
        FormWindowCustom form = new FormWindowCustom(configManager.getForms().getString("register.title"));
        form.addElement(new ElementLabel(configManager.getForms().getString("register.label")));
        form.addElement(new ElementInput("", configManager.getForms().getString("register.input")));
        player.showFormWindow(form, 1011);
    }

    public void showForgotPasswordForm(Player player) {
        FormWindowSimple form = new FormWindowSimple(
                configManager.getForms().getString("forgot-password.title"),
                configManager.getForms().getString("forgot-password.label")
        );
        form.addButton(new ElementButton(configManager.getForms().getString("forgot-password.button-try-again")));
        form.addButton(new ElementButton(configManager.getForms().getString("forgot-password.button-leave")));
        player.showFormWindow(form, 1010);
    }

    public void showChangePasswordForm(Player player) {
        FormWindowCustom form = new FormWindowCustom(configManager.getForms().getString("change-password.title"));
        form.addElement(new ElementLabel(configManager.getForms().getString("change-password.label")));
        form.addElement(new ElementInput("", configManager.getForms().getString("change-password.input-old")));
        form.addElement(new ElementInput("", configManager.getForms().getString("change-password.input-new")));
        form.addElement(new ElementInput("", configManager.getForms().getString("change-password.input-repeat")));
        player.showFormWindow(form, 1013);
    }
}