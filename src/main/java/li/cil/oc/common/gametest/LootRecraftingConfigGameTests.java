package li.cil.oc.common.gametest;

import com.google.gson.JsonElement;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModLootDisks;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class LootRecraftingConfigGameTests {
    private static final ResourceLocation RECIPE = ResourceLocation.fromNamespaceAndPath("neoopencomputers", "loot_disk_cycling");

    @GameTest(template = "empty")
    public static void lootSettingControlsActualRecipeReloads(GameTestHelper helper) throws Exception {
        final boolean previous = ModSettings.lootRecrafting();
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(RECIPE).isPresent() == previous, "Startup recipe set disagrees with the loaded loot recrafting setting");
        try {
            final var manager = new Probe(helper.getLevel().registryAccess());
            final var resources = helper.getLevel().getServer().getResourceManager();
            final var json = json(resources);
            for (boolean enabled : new boolean[]{false, true, false}) {
                ModSettings.LOOT_RECRAFTING.set(enabled);
                manager.load(json, resources);
                helper.assertTrue(!manager.hadErrorsLoading() && manager.byKey(RECIPE).isPresent() == enabled, "Loot recrafting config did not control real recipe loading/reloading");
                helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, input(), helper.getLevel()).isPresent() == enabled, "Crafting lookup ignored the server's loaded recipe set");
            }
        } finally { ModSettings.LOOT_RECRAFTING.set(previous); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipePacketPreservesServerChoiceDespiteLocalConfig(GameTestHelper helper) throws Exception {
        final boolean previous = ModSettings.lootRecrafting();
        try {
            final var resources = helper.getLevel().getServer().getResourceManager();
            final var json = json(resources);
            for (boolean serverEnabled : new boolean[]{false, true}) {
                ModSettings.LOOT_RECRAFTING.set(serverEnabled);
                final var server = new Probe(helper.getLevel().registryAccess()); server.load(json, resources);
                final var packet = new ClientboundUpdateRecipesPacket(server.getRecipes());
                ModSettings.LOOT_RECRAFTING.set(!serverEnabled);
                final var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
                try {
                    ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(buffer, packet);
                    final var received = ClientboundUpdateRecipesPacket.STREAM_CODEC.decode(buffer);
                    final var clientRecipes = new RecipeManager(helper.getLevel().registryAccess());
                    clientRecipes.replaceRecipes(received.getRecipes());
                    helper.assertTrue(clientRecipes.byKey(RECIPE).isPresent() == serverEnabled, "Local config changed the server's serialized recipe set");
                    helper.assertTrue(clientRecipes.getRecipeFor(RecipeType.CRAFTING, input(), helper.getLevel()).isPresent() == serverEnabled, "Received crafting lookup disagreed with the server's choice");
                } finally { buffer.release(); }
            }
        } finally { ModSettings.LOOT_RECRAFTING.set(previous); }
        helper.succeed();
    }

    private static CraftingInput input() {
        return CraftingInput.of(2, 1, List.of(ModLootDisks.stacksForCycling().getFirst(), new ItemStack(ModItems.WRENCH.get())));
    }

    private static JsonElement json(ResourceManager resources) throws Exception {
        try (var reader = resources.getResource(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "recipe/loot_disk_cycling.json")).orElseThrow().openAsReader()) {
            return GsonHelper.parse(reader);
        }
    }

    private static final class Probe extends RecipeManager {
        Probe(RegistryAccess registries) {
            super(registries); injectContext(ICondition.IContext.EMPTY, registries);
        }
        void load(JsonElement json, ResourceManager resources) {
            apply(Map.of(RECIPE, json), resources, InactiveProfiler.INSTANCE);
        }
    }
}
