package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.common.network.LootDiskClientState;
import li.cil.oc.common.network.LootDiskMetadataPayload;
import li.cil.oc.common.network.LootDiskNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class LootDiskMetadataGameTests {
    @GameTest(template = "empty")
    public static void enablingAnExcludedDiskAppendsToTheExistingCycle(GameTestHelper helper) {
        final var registry = registry();
        registry.registerFloppy("First excluded", DyeColor.BLUE, "metadata:first", () -> null, false);
        final var second = registry.registerFloppy("Second", DyeColor.RED, "metadata:second", () -> null, true);
        final var first = registry.registerFloppy("First enabled", DyeColor.GREEN, "metadata:first", () -> null, true);
        helper.assertTrue(ItemStack.listMatches(registry.disksForCycling(), List.of(second, first)), "Enabling an earlier excluded definition reordered the established cycle");
        registry.registerFloppy("Second excluded", DyeColor.RED, "metadata:second", () -> null, false);
        final var secondAgain = registry.registerFloppy("Second enabled", DyeColor.BLUE, "metadata:second", () -> null, true);
        helper.assertTrue(ItemStack.listMatches(registry.disksForCycling(), List.of(first, secondAgain)) && registry.registeredFloppies().size() == 2, "Re-enabling a disk duplicated its metadata or changed eligible registration order");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void metadataCodecIncludesOptOutsWithoutSharingServerTemplates(GameTestHelper helper) {
        final var previousApi = API.items;
        final var previousClient = LootDiskClientState.stacks();
        try {
            final var server = registry(); API.items = server;
            final var factoryCalls = new AtomicInteger();
            final var first = server.registerFloppy("First", DyeColor.BLUE, "metadata:first", () -> { factoryCalls.incrementAndGet(); return null; }, true);
            final var excluded = server.registerFloppy("Excluded", DyeColor.RED, "metadata:excluded", () -> { factoryCalls.incrementAndGet(); return null; }, false);
            final var expected = List.of(first.copy(), excluded.copy());
            first.setCount(0); excluded.remove(DataComponents.CUSTOM_DATA);
            final var packets = LootDiskNetworking.snapshot();
            helper.assertTrue(packets.size() == 3 && packets.getFirst().stack().isEmpty(), "Login metadata lacks reset or opted-out definitions");
            for (final var payload : packets) {
                final var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
                try {
                    LootDiskMetadataPayload.STREAM_CODEC.encode(buffer, payload);
                    final var decoded = LootDiskMetadataPayload.STREAM_CODEC.decode(buffer);
                    LootDiskClientState.accept(decoded);
                    decoded.stack().setCount(0);
                } finally { buffer.release(); }
            }
            helper.assertTrue(ItemStack.listMatches(expected, LootDiskClientState.stacks()), "Metadata round trip lost label/color/factory/opt-out components or shared a payload stack");
            LootDiskClientState.stacks().getFirst().setCount(0);
            server.registeredFloppies().getFirst().setCount(0);
            helper.assertTrue(ItemStack.listMatches(expected, server.registeredFloppies()) && server.disksForCycling().size() == 1 && factoryCalls.get() == 0, "Metadata altered registration templates/cycling or invoked a filesystem factory");
            LootDiskClientState.accept(new LootDiskMetadataPayload(new ItemStack(ModItems.FLOPPY.get())));
            LootDiskClientState.accept(new LootDiskMetadataPayload(new ItemStack(net.minecraft.world.item.Items.STONE)));
            helper.assertTrue(ItemStack.listMatches(expected, LootDiskClientState.stacks()), "Invalid metadata became a registered loot disk");
        } finally { API.items = previousApi; restore(previousClient); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void metadataBatchesAndDetachesAcrossClientRuntimeReplacement(GameTestHelper helper) throws Exception {
        final var previousClient = LootDiskClientState.stacks();
        final var registry = registry();
        final var disk = registry.registerFloppy("Batch", DyeColor.BLUE, "metadata:batch", () -> null, false);
        final var firstCalls = new AtomicInteger();
        final var replacementCalls = new AtomicInteger();
        try {
            LootDiskClientState.reset();
            final var first = LootDiskClientState.subscribe(firstCalls::incrementAndGet);
            try {
                for (int index = 0; index < 5; index++) LootDiskClientState.accept(new LootDiskMetadataPayload(disk));
                helper.assertTrue(firstCalls.get() == 0, "Network receive mutated runtime ingredients before the client tick");
                LootDiskClientState.flush(); LootDiskClientState.flush();
                helper.assertTrue(firstCalls.get() == 1, "One tick did not batch duplicate metadata");
                disk.set(DataComponents.CUSTOM_NAME, Component.literal("Updated"));
                LootDiskClientState.accept(new LootDiskMetadataPayload(disk));
                first.close(); first.close();
                try (final var replacement = LootDiskClientState.subscribe(replacementCalls::incrementAndGet)) {
                    LootDiskClientState.flush();
                    helper.assertTrue(firstCalls.get() == 1 && replacementCalls.get() == 1, "Queued metadata touched an obsolete runtime or was lost before replacement attached");
                    LootDiskClientState.reset(); LootDiskClientState.flush();
                    helper.assertTrue(replacementCalls.get() == 2 && LootDiskClientState.stacks().isEmpty(), "Disconnect retained the previous server's metadata");
                }
                final AutoCloseable[] later = new AutoCloseable[1];
                try (final var earlier = LootDiskClientState.subscribe(() -> {
                    try { later[0].close(); } catch (Exception failure) { throw new RuntimeException(failure); }
                })) {
                    later[0] = LootDiskClientState.subscribe(replacementCalls::incrementAndGet);
                    LootDiskClientState.flush();
                    helper.assertTrue(replacementCalls.get() == 2, "A subscription detached during dispatch still received stale work");
                } finally { if (later[0] != null) later[0].close(); }
            } finally { first.close(); }
        } finally { restore(previousClient); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeContentsMergeAuthoritativeMetadataWithoutChangingLocalFactories(GameTestHelper helper) {
        final var previousApi = API.items;
        final var previousClient = LootDiskClientState.stacks();
        try {
            final var server = registry(); API.items = server;
            final var authoritative = server.registerFloppy("Server label", DyeColor.RED, "metadata:shared", () -> null, false);
            final var remoteOnly = server.registerFloppy("Server only", DyeColor.GREEN, "metadata:remote", () -> null, false);
            final var packets = LootDiskNetworking.snapshot();
            final var local = registry(); API.items = local;
            final var original = local.registerFloppy("Local label", DyeColor.BLUE, "metadata:shared", () -> null, true);
            final var localOnly = local.registerFloppy("Local only", DyeColor.GRAY, "metadata:local", () -> null, true);
            final var bios = local.registerEEPROM("BIOS", new byte[]{1, 2}, new byte[]{3}, true);
            packets.forEach(LootDiskClientState::accept);
            final var output = new ArrayList<ItemStack>();
            ModCreativeTabs.addRegisteredStacks((stack, visibility) -> output.add(stack));
            helper.assertTrue(output.size() == 4 && output.stream().anyMatch(stack -> ItemStack.matches(stack, bios)), "Creative sync dropped the EEPROM or duplicated a factory");
            for (final var expected : List.of(authoritative, remoteOnly, localOnly)) {
                helper.assertTrue(output.stream().anyMatch(stack -> ItemStack.matches(stack, expected)), "Creative metadata lost a server/opt-out/local item");
            }
            helper.assertTrue(local.disksForCycling().size() == 2 && ItemStack.matches(local.registeredFloppies().getFirst(), original), "Incoming metadata overwrote the integrated server's local registration");
            output.forEach(stack -> stack.setCount(0));
            helper.assertTrue(!LootDiskClientState.stacks().getFirst().isEmpty() && !local.registeredFloppies().getFirst().isEmpty(), "Creative output shared private metadata templates");
        } finally { API.items = previousApi; restore(previousClient); }
        helper.succeed();
    }

    private static ItemRegistry registry() {
        final var registry = new ItemRegistry();
        registry.register(ModContentIds.FLOPPY, null, ModItems.FLOPPY.get());
        registry.register(ModContentIds.EEPROM, null, ModItems.EEPROM.get());
        return registry;
    }

    private static void restore(List<ItemStack> previous) {
        LootDiskClientState.reset();
        previous.forEach(stack -> LootDiskClientState.accept(new LootDiskMetadataPayload(stack)));
    }
}
