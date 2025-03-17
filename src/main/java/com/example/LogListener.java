package com.example;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LogListener implements Runnable {
    private final JavaPlugin plugin;
    private final Map<String, TriggerRule> triggerRules = new HashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private Thread watchThread;
    private long lastReadPosition;
    private int globalCooldown;

    // 触发规则数据结构
    private static class TriggerRule {
        final String command;
        final int cooldown;

        TriggerRule(String command, int cooldown) {
            this.command = command;
            this.cooldown = cooldown;
        }
    }

    public LogListener(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
        File logFile = new File("logs/latest.log");
        this.lastReadPosition = logFile.exists() ? logFile.length() : 0;
    }

    public void reloadConfig() {
        triggerRules.clear();
        cooldowns.clear();

        globalCooldown = plugin.getConfig().getInt("global-cooldown", 0);
        ConfigurationSection triggers = plugin.getConfig().getConfigurationSection("triggers");

        if (triggers != null) {
            for (String key : triggers.getKeys(false)) {
                Object value = triggers.get(key);

                if (value instanceof String) {
                    // 旧版配置格式：直接命令
                    triggerRules.put(key, new TriggerRule((String) value, globalCooldown));
                } else if (value instanceof ConfigurationSection) {
                    // 新版配置格式：带独立参数
                    ConfigurationSection section = (ConfigurationSection) value;
                    String command = section.getString("command");
                    int cooldown = section.getInt("cooldown", globalCooldown);
                    triggerRules.put(key, new TriggerRule(command, cooldown));
                }
            }
        }
    }

    public void startWatching() {
        if (watchThread == null || !watchThread.isAlive()) {
            running = true;
            watchThread = new Thread(this);
            watchThread.start();
        }
    }

    public void stopWatching() {
        running = false;
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }

    @Override
    public void run() {
        Path logPath = Paths.get("logs/latest.log");
        try {
            while (running) {
                if (!Files.exists(logPath)) {
                    Thread.sleep(5000);
                    continue;
                }

                long fileSize = Files.size(logPath);
                if (fileSize < lastReadPosition) lastReadPosition = 0;

                if (fileSize > lastReadPosition) {
                    try (RandomAccessFile raf = new RandomAccessFile(logPath.toFile(), "r")) {
                        raf.seek(lastReadPosition);
                        String line;
                        while ((line = raf.readLine()) != null) {
                            processLine(line);
                        }
                        lastReadPosition = raf.getFilePointer();
                    }
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("日志监听错误: " + e.getMessage());
        }
    }

    private void processLine(String line) {
        line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        for (Map.Entry<String, TriggerRule> entry : triggerRules.entrySet()) {
            if (line.contains(entry.getKey())) {
                checkAndExecute(entry.getKey(), entry.getValue());
            }
        }
    }

    private void checkAndExecute(String keyword, TriggerRule rule) {
        long now = System.currentTimeMillis();
        long lastTrigger = cooldowns.getOrDefault(keyword, 0L);
        int remaining = (int) ((now - lastTrigger) / 1000);

        if (remaining >= rule.cooldown) {
            cooldowns.put(keyword, now);
            executeCommand(rule.command);
        } else {
            plugin.getLogger().info("§e触发词冷却中，剩余 " + (rule.cooldown - remaining) + "秒");
        }
    }

    private void executeCommand(String command) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            plugin.getLogger().info("§a已执行命令: " + command);
        });
    }

    public int getGlobalCooldown() {
        return globalCooldown;
    }

    public int getRuleCount() {
        return triggerRules.size();
    }
}