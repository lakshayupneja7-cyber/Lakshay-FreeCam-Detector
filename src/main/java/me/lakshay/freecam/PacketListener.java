package me.lakshay.freecam;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PacketListener {

    private final Plugin plugin;
    private final ProtocolManager manager;

    // avoid repeating same logs too much
    private final Set<String> seenLogs = new HashSet<>();

    public PacketListener(Plugin plugin) {
        this.plugin = plugin;
        this.manager = ProtocolLibrary.getProtocolManager();
    }

    public void register() {

        // PLAY phase payloads
        manager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Client.CUSTOM_PAYLOAD
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handlePayload(event, "PLAY");
            }
        });

        // CONFIG phase payloads
        manager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Configuration.Client.CUSTOM_PAYLOAD
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handlePayload(event, "CONFIG");
            }
        });
    }

    private void handlePayload(PacketEvent event, String phase) {
        Player player = event.getPlayer();

        try {
            String channel = readChannelSafely(event);
            if (channel == null || channel.isBlank()) {
                return;
            }

            byte[] rawData = readPayloadBytes(event);
            String dataPreview = buildDataPreview(rawData);

            logOnce(
                    player.getUniqueId(),
                    player.getName(),
                    phase,
                    channel,
                    dataPreview
            );

            String lowerChannel = channel.toLowerCase();

            // already confirmed from your logs
            if (lowerChannel.contains("autototem-fabric")) {
                player.kickPlayer("Please remove AutoTotem and rejoin.");
                return;
            }

            // inspect common interesting channels more deeply
            if (lowerChannel.contains("minecraft:brand")) {
                String brand = tryReadUtf8(rawData);
                if (!brand.isBlank()) {
                    plugin.getLogger().info("[Brand] " + player.getName() + " -> " + brand);
                }
            }

            if (lowerChannel.contains("minecraft:register")) {
                String registerData = tryReadUtf8(rawData);
                if (!registerData.isBlank()) {
                    plugin.getLogger().info("[RegisterData] " + player.getName() + " -> " + sanitize(registerData));
                }
            }

            if (lowerChannel.contains("balm:mod_list")) {
                String modListData = tryReadUtf8(rawData);
                if (!modListData.isBlank()) {
                    plugin.getLogger().info("[BalmModList] " + player.getName() + " -> " + sanitize(modListData));
                }
            }

        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to inspect " + phase + " custom payload: " + ex.getMessage());
        }
    }

    private void logOnce(UUID uuid, String playerName, String phase, String channel, String dataPreview) {
        String key = uuid + "|" + phase + "|" + channel.toLowerCase();

        if (seenLogs.add(key)) {
            plugin.getLogger().info(
                    "[ChannelLog] " + playerName + " -> " + phase + " -> " + channel + " -> " + dataPreview
            );
        }
    }

    private String readChannelSafely(PacketEvent event) {
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
            Object first = event.getPacket().getModifier().read(0);
            if (first != null) {
                return first.toString();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private byte[] readPayloadBytes(PacketEvent event) {
        // Try byte arrays directly
        try {
            if (!event.getPacket().getByteArrays().getValues().isEmpty()) {
                byte[] data = event.getPacket().getByteArrays().read(0);
                if (data != null) {
                    return data;
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback: try packet modifier values and stringify if needed
        try {
            for (Object value : event.getPacket().getModifier().getValues()) {
                if (value instanceof byte[]) {
                    return (byte[]) value;
                }
            }
        } catch (Exception ignored) {
        }

        return new byte[0];
    }

    private String buildDataPreview(byte[] data) {
        if (data == null || data.length == 0) {
            return "data=<empty>";
        }

        String utf = tryReadUtf8(data);
        if (!utf.isBlank()) {
            return "utf8=" + sanitize(utf);
        }

        return "bytes=" + toHexPreview(data, 48);
    }

    private String tryReadUtf8(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        String s = new String(data, StandardCharsets.UTF_8);

        // strip obvious nulls / junk for logging
        s = s.replace('\u0000', ' ').trim();

        // if mostly unreadable, ignore
        int printable = 0;
        for (char c : s.toCharArray()) {
            if (c >= 32 && c < 127) {
                printable++;
            }
        }

        if (s.isEmpty()) {
            return "";
        }

        double ratio = (double) printable / (double) s.length();
        return ratio >= 0.50 ? s : "";
    }

    private String sanitize(String s) {
        s = s.replace("\n", "\\n").replace("\r", "\\r");
        if (s.length() > 200) {
            return s.substring(0, 200) + "...";
        }
        return s;
    }

    private String toHexPreview(byte[] data, int maxLen) {
        StringBuilder sb = new StringBuilder();
        int len = Math.min(data.length, maxLen);

        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X", data[i]));
            if (i < len - 1) {
                sb.append(' ');
            }
        }

        if (data.length > maxLen) {
            sb.append(" ...");
        }

        return sb.toString();
    }
}
