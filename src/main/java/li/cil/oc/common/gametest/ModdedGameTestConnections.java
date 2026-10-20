package li.cil.oc.common.gametest;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Give headless mock players the installed mods' channels through NeoForge's supported test helper. */
final class ModdedGameTestConnections {
    private static boolean registered;

    static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ModdedGameTestConnections::joining);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ModdedGameTestConnections::tracking);
    }

    private static void joining(final EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) configure(player);
    }

    private static void tracking(final PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) configure(player);
    }

    private static void configure(final ServerPlayer player) {
        if (player.connection != null) {
            final var connection = player.connection.getConnection();
            if (connection.isMemoryConnection() || connection.channel() instanceof io.netty.channel.embedded.EmbeddedChannel) {
                NetworkRegistry.configureMockConnection(connection);
            }
        }
    }
}
