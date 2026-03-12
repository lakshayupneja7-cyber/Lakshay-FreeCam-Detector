package me.lakshay.freecam;

import org.bukkit.plugin.java.JavaPlugin;

public final class FreeCamDetector extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Lakshay FreeCam Detector Enabled");
        new PacketListener(this).register();
    }

    @Override
    public void onDisable() {
        getLogger().info("Lakshay FreeCam Detector Disabled");
    }
}
