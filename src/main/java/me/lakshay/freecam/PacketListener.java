package me.lakshay.freecam;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PacketListener {

    private final Plugin plugin;
    private final ProtocolManager manager;

    public PacketListener(Plugin plugin) {

        this.plugin = plugin;
        this.manager = ProtocolLibrary.getProtocolManager();

    }

    public void register() {

        manager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Client.CUSTOM_PAYLOAD
        ) {

            @Override
            public void onPacketReceiving(PacketEvent event) {

                Player player = event.getPlayer();

                try {

                    String channel = event.getPacket().getStrings().read(0).toLowerCase();

                    if(channel.contains("freecam")) {

                        player.kickPlayer("Please remove FreeCam and rejoin.");

                    }

                } catch (Exception ignored) {}

            }

        });

    }

}
