package li.cil.oc.common.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualItemTest {
    @Test
    void manualItemIsUsableItem() throws NoSuchMethodException {
        final Method use = ManualItem.class.getMethod("use", Level.class, Player.class, InteractionHand.class);

        assertTrue(Item.class.isAssignableFrom(ManualItem.class));
        assertEquals(InteractionResultHolder.class, use.getReturnType());
    }

    @Test
    void manualItemConstructorUsesItemProperties() throws NoSuchMethodException {
        assertEquals(Item.Properties.class, ManualItem.class.getConstructor(Item.Properties.class).getParameterTypes()[0]);
    }
}
