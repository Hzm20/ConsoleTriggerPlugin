package com.example;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ConsoleTriggerPlugin extends JavaPlugin {
    private LogListener logListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        logListener = new LogListener(this);
        logListener.startWatching();

        getLogger().info("§a插件已加载！全局冷却: " + logListener.getGlobalCooldown() + "s");
    }

    @Override
    public void onDisable() {
        if (logListener != null) {
            logListener.stopWatching();
        }
        getLogger().info("§c插件已卸载");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("ctpreload")) {
            reloadConfig();
            logListener.reloadConfig();
            sender.sendMessage("§a配置已重载！当前规则数: " + logListener.getRuleCount());
            return true;
        }
        return false;
    }
}