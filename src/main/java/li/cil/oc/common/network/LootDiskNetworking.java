package li.cil.oc.common.network;

import li.cil.oc.api.API;
import li.cil.oc.common.ItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LootDiskNetworking {
    private static final Set<ServerPlayer> pendingLogins = new LinkedHashSet<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(LootDiskMetadataPayload.TYPE, LootDiskMetadataPayload.STREAM_CODEC,
            (payload, context) -> LootDiskClientState.accept(payload));
    }

    public static void registerServerEvents() {
        NeoForge.EVENT_BUS.addListener(LootDiskNetworking::onLogin);
        NeoForge.EVENT_BUS.addListener(LootDiskNetworking::onTick);
        NeoForge.EVENT_BUS.addListener(LootDiskNetworking::onStopped);
    }

    public static List<LootDiskMetadataPayload> snapshot() {
        return snapshot(net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer());
    }

    public static List<LootDiskMetadataPayload> snapshot(net.minecraft.server.MinecraftServer server) {
        final var packets = new ArrayList<LootDiskMetadataPayload>();
        packets.add(new LootDiskMetadataPayload(ItemStack.EMPTY));
        if (API.items instanceof ItemRegistry registry) {
            final var stacks = server == null ? registry.registeredFloppies() : li.cil.oc.common.WorldLootDisks.registeredDisks(server, registry);
            for (final var stack : stacks) packets.add(new LootDiskMetadataPayload(stack));
        }
        return List.copyOf(packets);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) pendingLogins.add(player);
    }

    private static void onTick(ServerTickEvent.Post event) {
        pendingLogins.removeIf(player -> {
            if (player.getServer() != event.getServer()) return false;
            if (!player.hasDisconnected() && event.getServer().getPlayerList().getPlayer(player.getUUID()) == player) {
                for (final var payload : snapshot(event.getServer())) TerminalNetworking.sendToPlayerIfSupported(player, payload);
            }
            return true;
        });
    }

    private static void onStopped(ServerStoppedEvent event) {
        pendingLogins.removeIf(player -> player.getServer() == event.getServer());
    }

    private LootDiskNetworking() {}
}
