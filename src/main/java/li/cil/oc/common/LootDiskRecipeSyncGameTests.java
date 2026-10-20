package li.cil.oc.common;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import li.cil.oc.api.API;
import li.cil.oc.common.recipe.LootDiskCyclingRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class LootDiskRecipeSyncGameTests {
    private static final ResourceLocation RECIPE = ResourceLocation.fromNamespaceAndPath("neoopencomputers", "loot_disk_cycling");

    @GameTest(template = "empty")
    public static void standardRecipeUpdatesCarryServerCyclesInsteadOfClientRegistrations(GameTestHelper helper) {
        final var previous = API.items;
        try {
            final var server = registry();
            final var factoryCalls = new AtomicInteger();
            final var first = disk(server, "first", true, factoryCalls);
            final var second = disk(server, "second", true, factoryCalls);
            disk(server, "excluded", false, factoryCalls);
            API.items = server;
            final var recipe = ModRecipeSerializers.LOOT_DISK_CYCLING.get().codec().codec()
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{\"category\":\"equipment\"}")).getOrThrow();
            final var json = ModRecipeSerializers.LOOT_DISK_CYCLING.get().codec().codec()
                .encodeStart(JsonOps.INSTANCE, recipe).getOrThrow().getAsJsonObject();
            helper.assertTrue(json.size() == 1 && json.get("category").getAsString().equals("equipment"), "Network snapshot changed the existing data-pack JSON schema");
            helper.assertTrue(ItemStack.matches(recipe.assemble(input(first), helper.getLevel().registryAccess()), second), "Server fixture did not use its registered cycle");

            final var client = registry();
            disk(client, "client-only", true, factoryCalls);
            disk(client, "first", false, factoryCalls);
            final var received = transfer(helper, recipe, server, client);
            helper.assertTrue(received.category() == CraftingBookCategory.EQUIPMENT, "Recipe packet lost its crafting-book category");
            helper.assertTrue(ItemStack.matches(received.assemble(input(first), helper.getLevel().registryAccess()), second), "Client registry replaced the server's transmitted cycling outputs");
            final var result = received.assemble(input(first), helper.getLevel().registryAccess());
            result.setCount(0);
            helper.assertTrue(ItemStack.matches(received.assemble(input(first), helper.getLevel().registryAccess()), second), "Crafting result mutation changed the synchronized template");

            API.items = server;
            final var third = disk(server, "third", true, factoryCalls);
            helper.assertTrue(ItemStack.matches(recipe.assemble(input(second), helper.getLevel().registryAccess()), third), "JSON/server recipe froze its registry before later registrations");
            helper.assertTrue(ItemStack.matches(received.assemble(input(second), helper.getLevel().registryAccess()), first), "Client snapshot changed when the integrated server registry changed");
            final var updated = transfer(helper, recipe, server, client);
            helper.assertTrue(ItemStack.matches(updated.assemble(input(second), helper.getLevel().registryAccess()), third), "A new standard recipe update retained the old cycling snapshot");
            final var forwarded = transfer(helper, received, client, server);
            helper.assertTrue(ItemStack.matches(forwarded.assemble(input(first), helper.getLevel().registryAccess()), second), "Re-encoding a received recipe replaced its server snapshot with local registrations");
            helper.assertTrue(server.disksForCycling().size() == 3 && client.disksForCycling().size() == 1 && factoryCalls.get() == 0, "Recipe metadata sync changed registries or invoked filesystem factories");
        } finally {
            API.items = previous;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyServerCycleDoesNotFallBackToPopulatedClientRegistrations(GameTestHelper helper) {
        final var previous = API.items;
        try {
            final var server = registry();
            final var client = registry();
            final var candidate = disk(client, "client-only", true, new AtomicInteger());
            final var received = transfer(helper, new LootDiskCyclingRecipe(CraftingBookCategory.MISC), server, client);
            helper.assertTrue(received.matches(input(candidate), helper.getLevel()), "Synchronized empty cycle changed valid loot-disk input matching");
            helper.assertTrue(received.assemble(input(candidate), helper.getLevel().registryAccess()).isEmpty(), "Empty server cycle fell back to client-local disks");
            helper.assertTrue(client.disksForCycling().size() == 1 && server.disksForCycling().isEmpty(), "Empty snapshot modified a live registration list");
        } finally {
            API.items = previous;
        }
        helper.succeed();
    }

    private static ItemRegistry registry() {
        final var registry = new ItemRegistry();
        registry.register(ModContentIds.FLOPPY, null, ModItems.FLOPPY.get());
        return registry;
    }

    private static ItemStack disk(ItemRegistry registry, String name, boolean cycling, AtomicInteger factoryCalls) {
        return registry.registerFloppy(name, DyeColor.BLUE, "sync:" + name, () -> {
            factoryCalls.incrementAndGet();
            return null;
        }, cycling);
    }

    private static CraftingInput input(ItemStack disk) {
        return CraftingInput.of(2, 1, List.of(disk, new ItemStack(ModItems.WRENCH.get())));
    }

    static LootDiskCyclingRecipe transfer(GameTestHelper helper, LootDiskCyclingRecipe recipe, ItemRegistry sending, ItemRegistry receiving) {
        final var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            API.items = sending;
            ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(buffer, new ClientboundUpdateRecipesPacket(List.of(new RecipeHolder<>(RECIPE, recipe))));
            API.items = receiving;
            final var packet = ClientboundUpdateRecipesPacket.STREAM_CODEC.decode(buffer);
            final var manager = new RecipeManager(helper.getLevel().registryAccess());
            manager.replaceRecipes(packet.getRecipes());
            return (LootDiskCyclingRecipe) manager.byKey(RECIPE).orElseThrow().value();
        } finally {
            buffer.release();
        }
    }
}
