package li.cil.oc.common.gametest;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class MockConnectionGameTests {
    private static boolean registered;

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!registered) {
            registered = true;
            // The deprecated vanilla helper logs in an EmbeddedChannel without a mod handshake.
            // Configure the official NeoForge test connection before mods sync mandatory payloads.
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, MockConnectionGameTests::beforeDatapackSync);
        }
        return List.of(new TestFunction("network", "mock_player_has_negotiated_mod_channels", "neoopencomputers:empty", 100, 0, true, MockConnectionGameTests::negotiatedChannels));
    }

    private static void beforeDatapackSync(OnDatapackSyncEvent event) {
        final var player = event.getPlayer();
        if (player != null && player.getGameProfile().getName().equals("test-mock-player")
            && player.connection.getConnection().channel() instanceof EmbeddedChannel) {
            NetworkRegistry.configureMockConnection(player.connection.getConnection());
        }
    }

    private static void negotiatedChannels(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            final var connection = player.connection.getConnection();
            helper.assertTrue(ChannelAttributes.getConnectionType(connection) == ConnectionType.NEOFORGE, "Test player has no NeoForge handshake");
            helper.assertTrue(ChannelAttributes.getPayloadSetup(connection) != null, "Test player has no negotiated channels");
            if (net.neoforged.fml.ModList.get().isLoaded("forestry")) {
                helper.assertTrue(NetworkRegistry.hasChannel(connection, ConnectionProtocol.PLAY, ResourceLocation.fromNamespaceAndPath("forestry", "taxon_sync")), "Forestry mandatory taxon channel missing from test connection");
            }
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }
}
