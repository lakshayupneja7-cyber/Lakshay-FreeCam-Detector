package me.lakshay.freecam;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PacketListener {

    private final Plugin plugin;
    private final ProtocolManager manager;

    // to avoid spamming the same channel again and again
    private final Set<String> seenLogs = new HashSet<>();

    public PacketListener(Plugin plugin) {
        this.plugin = plugin;
        this.manager = ProtocolLibrary.getProtocolManager();
    }

    public void register() {

        // 1) custom payload packets during play phase
        manager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Client.CUSTOM_PAYLOAD
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();

                try {
                    String channel = readChannelSafely(event);
                    if (channel == null || channel.isBlank()) {
                        return;
                    }

                    logOnce(player.getUniqueId(), player.getName(), "PLAY", channel);

                    // temporary example detection
                    String lc = channel.toLowerCase();
                    if (lc.contains("freecam") || lc.contains("camera")) {
                        player.kickPlayer("Please remove FreeCam and rejoin.");
                    }

                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to inspect PLAY custom payload: " + ex.getMessage());
                }
            }
        });

        // 2) custom payload packets during configuration/login-like phase
        manager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Configuration.Client.CUSTOM_PAYLOAD
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();

                try {
                    String channel = readChannelSafely(event);
                    if (channel == null || channel.isBlank()) {
                        return;
                    }

                    logOnce(player.getUniqueId(), player.getName(), "CONFIG", channel);

                    // temporary example detection
                    String lc = channel.toLowerCase();
                    if (lc.contains("freecam") || lc.contains("camera")) {
                        player.kickPlayer("Please remove FreeCam and rejoin.");
                    }

                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to inspect CONFIG custom payload: " + ex.getMessage());
                }
            }
        });
    }

    private void logOnce(UUID uuid, String playerName, String phase, String channel) {
        String key = uuid + "|" + phase + "|" + channel.toLowerCase();

        if (seenLogs.add(key)) {
            plugin.getLogger().info("[ChannelLog] " + playerName + " -> " + phase + " -> " + channel);
        }
    }

    private String readChannelSafely(PacketEvent event) {
        // ProtocolLib packet structure changes across versions,
        // so try a few safe ways.
        try {
            if (!event.getPacket().getMinecraftKeys().getValues().isEmpty()) {
                return event.getPacket().getMinecraftKeys().read(0).getFullKey();
            }
        } catch (Exception ignored) {
        }

        try {
            if (!event.getPacket().getStrings().getValues().isEmpty()) {
                return event.getPacket().getStrings().read(0);
            }
        } catch (Exception ignored) {
        }

        try {
            Object modifierValue = event.getPacket().getModifier().read(0);
            if (modifierValue != null) {
                return modifierValue.toString();
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
