package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CraftingUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "crafting";
    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;

    private final Agent host;

    public CraftingUpgradeEnvironment(final Agent host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Assembly controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "MultiCombinator-9S"
        );
    }

    @Callback(doc = "function([count:number]):number -- Tries to craft the specified number of items in the top left area of the inventory.")
    public Object[] craft(final Context context, final Arguments arguments) {
        final int wantedCount = Math.max(0, Math.min(64, arguments.optInteger(0, 64)));
        final Level level = host.world();
        final Container inventory = host.mainInventory();
        if (wantedCount <= 0 || level == null || inventory == null) {
            return new Object[]{false, 0};
        }

        final Optional<RecipeHolder<CraftingRecipe>> initialRecipe = findRecipe(level, inventory);
        if (initialRecipe.isEmpty()) {
            return new Object[]{false, 0};
        }

        final ResourceLocation initialRecipeId = initialRecipe.get().id();
        int crafted = 0;
        while (crafted < wantedCount) {
            final Optional<RecipeHolder<CraftingRecipe>> currentRecipe = findRecipe(level, inventory);
            if (currentRecipe.isEmpty() || !initialRecipeId.equals(currentRecipe.get().id())) {
                break;
            }

            final CraftingInput input = craftingInput(inventory);
            final CraftingRecipe recipe = currentRecipe.get().value();
            final ItemStack result = recipe.assemble(input, level.registryAccess());
            if (result.isEmpty()) {
                break;
            }

            final List<ItemStack> snapshot = snapshot(inventory);
            if (!consumeIngredients(level, inventory, input) || !insertStack(inventory, result.copy())) {
                restore(inventory, snapshot);
                break;
            }

            crafted += Math.max(1, result.getCount());
            host.markChanged();
        }
        return new Object[]{crafted > 0, crafted};
    }

    private static Optional<RecipeHolder<CraftingRecipe>> findRecipe(final Level level, final Container inventory) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInput(inventory), level);
    }

    private static CraftingInput craftingInput(final Container inventory) {
        final List<ItemStack> stacks = new ArrayList<>(GRID_WIDTH * GRID_HEIGHT);
        for (int slot = 0; slot < GRID_WIDTH * GRID_HEIGHT; slot++) {
            final int parentSlot = toParentSlot(slot);
            stacks.add(parentSlot < inventory.getContainerSize() ? inventory.getItem(parentSlot).copy() : ItemStack.EMPTY);
        }
        return CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, stacks);
    }

    private static boolean consumeIngredients(final Level level, final Container inventory, final CraftingInput input) {
        final NonNullList<ItemStack> remaining = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, input, level);
        for (int slot = 0; slot < GRID_WIDTH * GRID_HEIGHT; slot++) {
            final int parentSlot = toParentSlot(slot);
            if (parentSlot >= inventory.getContainerSize()) {
                continue;
            }

            final ItemStack stack = inventory.getItem(parentSlot);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(parentSlot, ItemStack.EMPTY);
                }
            }

            final ItemStack remainder = slot < remaining.size() ? remaining.get(slot) : ItemStack.EMPTY;
            if (!remainder.isEmpty() && !insertStack(inventory, remainder.copy())) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertStack(final Container inventory, final ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack existing = inventory.getItem(slot);
            if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                final int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, stack.copy());
                stack.setCount(0);
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> snapshot(final Container inventory) {
        final List<ItemStack> snapshot = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            snapshot.add(inventory.getItem(slot).copy());
        }
        return snapshot;
    }

    private static void restore(final Container inventory, final List<ItemStack> snapshot) {
        for (int slot = 0; slot < snapshot.size(); slot++) {
            inventory.setItem(slot, snapshot.get(slot).copy());
        }
    }

    private static int toParentSlot(final int slot) {
        final int column = slot % GRID_WIDTH;
        final int row = slot / GRID_WIDTH;
        return row * 4 + column;
    }
}
