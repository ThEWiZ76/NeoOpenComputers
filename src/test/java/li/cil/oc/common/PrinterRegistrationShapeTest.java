package li.cil.oc.common;

import li.cil.oc.common.block.PrinterBlock;
import li.cil.oc.common.blockentity.PrinterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrinterRegistrationShapeTest {
    @Test
    void printerBlockIsGuiEntityBlockLikeUpstream() throws Exception {
        final Constructor<PrinterBlock> constructor = PrinterBlock.class.getConstructor(BlockBehaviour.Properties.class);
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/PrinterBlock.java"));

        assertTrue(Block.class.isAssignableFrom(PrinterBlock.class));
        assertTrue(HorizontalDirectionalBlock.class.isAssignableFrom(PrinterBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(PrinterBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
        assertTrue(source.contains("useWithoutItem"));
        assertTrue(source.contains("player.openMenu(printer)"));
    }

    @Test
    void printerBlockEntityProvidesMenuLikeUpstream() throws NoSuchMethodException {
        final Constructor<PrinterBlockEntity> constructor = PrinterBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(MenuProvider.class.isAssignableFrom(PrinterBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
    }

    @Test
    void printerMenuIsRegistered() throws Exception {
        final String idsSource = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModContentIds.java"));
        final String menusSource = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModMenus.java"));
        final String clientSource = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(idsSource.contains("PRINTER_MENU"));
        assertTrue(menusSource.contains("MenuType<PrinterMenu>"));
        assertTrue(clientSource.contains("event.register(ModMenus.PRINTER.get(), PrinterScreen::new)"));
    }
}
