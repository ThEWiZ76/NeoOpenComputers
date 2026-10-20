package li.cil.oc.common;

import li.cil.oc.api.API;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class RegisteredItemCreativeGameTests {
    @GameTest(template = "empty")
    public static void callerCannotChangeRegisteredFloppyCreativeTemplate(GameTestHelper helper) {
        final var registry = new ItemRegistry();
        registry.register(ModContentIds.FLOPPY, null, ModItems.FLOPPY.get());
        final var returned = registry.registerFloppy("Creative fixture", DyeColor.BLUE, () -> null, true);
        final var expected = returned.copy();
        returned.set(DataComponents.CUSTOM_NAME, Component.literal("Changed by addon"));
        returned.remove(DataComponents.CUSTOM_DATA);
        returned.setCount(0);
        assertCreativeTemplate(helper, registry, expected);
        helper.assertTrue(ItemStack.matches(registry.disksForCycling().getFirst(), expected), "Caller changed the floppy cycling template");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void callerCannotChangeRegisteredEepromCreativeTemplate(GameTestHelper helper) {
        final var registry = new ItemRegistry();
        registry.register(ModContentIds.EEPROM, null, ModItems.EEPROM.get());
        final var returned = registry.registerEEPROM("Creative BIOS", new byte[]{1, 2, 3}, new byte[]{4, 5}, true);
        final var expected = returned.copy();
        returned.set(DataComponents.CUSTOM_NAME, Component.literal("Changed by addon"));
        returned.remove(DataComponents.CUSTOM_DATA);
        returned.setCount(0);
        assertCreativeTemplate(helper, registry, expected);
        helper.succeed();
    }

    private static void assertCreativeTemplate(GameTestHelper helper, ItemRegistry registry, ItemStack expected) {
        final var first = creativeEntries(registry);
        helper.assertTrue(first.size() == 1 && ItemStack.matches(first.getFirst(), expected), "Addon changed the registered creative template");
        first.getFirst().remove(DataComponents.CUSTOM_DATA);
        first.getFirst().setCount(0);
        final var second = creativeEntries(registry);
        helper.assertTrue(second.size() == 1 && ItemStack.matches(second.getFirst(), expected), "Creative entry mutation changed subsequent menu builds");
    }

    private static List<ItemStack> creativeEntries(ItemRegistry registry) {
        final var previous = API.items;
        final var entries = new ArrayList<ItemStack>();
        try {
            API.items = registry;
            ModCreativeTabs.addRegisteredStacks((stack, visibility) -> entries.add(stack));
        } finally {
            API.items = previous;
        }
        return entries;
    }
}
