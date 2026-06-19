package li.cil.oc.api.detail;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ItemInfoTest {
    @Test
    void itemInfoUsesModernBlockItemAndStackTypes() throws NoSuchMethodException {
        Method block = ItemInfo.class.getMethod("block");
        Method item = ItemInfo.class.getMethod("item");
        Method stack = ItemInfo.class.getMethod("createItemStack", int.class);
        ItemInfo info = new TestItemInfo();

        assertEquals(Block.class, block.getReturnType());
        assertEquals(Item.class, item.getReturnType());
        assertEquals(ItemStack.class, stack.getReturnType());
        assertEquals("test", info.name());
        assertNull(info.block());
        assertNull(info.item());
        assertNull(info.createItemStack(1));
    }

    private static final class TestItemInfo implements ItemInfo {
        @Override
        public String name() {
            return "test";
        }

        @Override
        public Block block() {
            return null;
        }

        @Override
        public Item item() {
            return null;
        }

        @Override
        public ItemStack createItemStack(final int size) {
            return null;
        }
    }
}
