package li.cil.oc.common;

import li.cil.oc.common.block.AdapterBlock;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdapterRegistrationShapeTest {
    @Test
    void adapterBlockIsGuiEntityBlockLikeUpstream() throws Exception {
        final Constructor<AdapterBlock> constructor = AdapterBlock.class.getConstructor(BlockBehaviour.Properties.class);
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/AdapterBlock.java"));

        assertTrue(Block.class.isAssignableFrom(AdapterBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(AdapterBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
        assertTrue(source.contains("useWithoutItem"));
        assertTrue(source.contains("player.openMenu(adapter)"));
    }

    @Test
    void adapterBlockEntityProvidesMenuLikeUpstream() throws NoSuchMethodException {
        final Constructor<AdapterBlockEntity> constructor = AdapterBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(MenuProvider.class.isAssignableFrom(AdapterBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
    }

    @Test
    void adapterMenuIsRegistered() throws Exception {
        final String idsSource = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModContentIds.java"));
        final String menusSource = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModMenus.java"));
        final String clientSource = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(idsSource.contains("ADAPTER_MENU"));
        assertTrue(menusSource.contains("MenuType<AdapterMenu>"));
        assertTrue(clientSource.contains("event.register(ModMenus.ADAPTER.get(), AdapterScreen::new)"));
    }
}
