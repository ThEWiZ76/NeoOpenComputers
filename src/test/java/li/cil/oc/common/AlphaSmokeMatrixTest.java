package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AlphaSmokeMatrixTest {
    @Test
    void alphaSmokeMatrixMapsChecklistToEvidence() throws IOException {
        final Path matrixPath = Path.of("ALPHA_SMOKE_MATRIX.md");
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(Files.isRegularFile(matrixPath), "Alpha smoke matrix must exist before first alpha sharing");
        assertTrue(readme.contains("[Alpha Smoke Matrix](ALPHA_SMOKE_MATRIX.md)"),
            "README must link alpha smoke matrix");

        final String matrix = Files.readString(matrixPath);
        for (final String requiredArea : new String[]{
            "OpenOS boot",
            "Lua/OpenOS API loading",
            "Computer case storage persistence",
            "Disk-drive floppy persistence",
            "Redstone",
            "Modem",
            "Inventory",
            "Tank",
            "Transposer",
            "Microcontrollers",
            "Robots",
            "Drones",
            "Power and charging",
            "Texture picker",
            "Printer and print",
            "Manual and packaging"
        }) {
            assertTrue(matrix.contains(requiredArea), "Alpha smoke matrix missing area: " + requiredArea);
        }

        assertTrue(matrix.contains("Automated evidence"), "Matrix must distinguish automated evidence");
        assertTrue(matrix.contains("Manual alpha smoke"), "Matrix must distinguish manual alpha smoke");
        assertTrue(matrix.contains("Screen world rendering stays frozen"),
            "Matrix must keep the screen loop-control rule visible");
        assertTrue(matrix.contains("VISUAL_SMOKE_RUNBOOK.md"),
            "Matrix must point manual visual proof to the visual smoke runbook");
        for (final String screenshotName : new String[]{
            "01-openos-prompt.png",
            "02-computer-gui.png",
            "03-screen-after-reload.png",
            "04-creative-tab.png",
            "05-manual.png",
            "06-robot.png",
            "07-drone.png",
            "08-printer-print.png"
        }) {
            assertTrue(matrix.contains(screenshotName), "Matrix missing required visual proof: " + screenshotName);
        }
        for (final String gameTestName : new String[]{
            "computerRunsWithLuaBiosAndOpenOsFloppy",
            "BiosResourceTest.bundledOpenOsProvidesRequireAndComponentPrimaryConvenience",
            "BiosResourceTest.bundledLuaBiosDoesNotProvideOpenOsRequire",
            "tier1ComputerWithNetworkCardBootsOpenOsHardDiskToLiveStyleScreenWall",
            "computerCaseStorageStateSurvivesNbtReloadForFirstSmoke",
            "rackDiskDriveWritableFloppyStateSurvivesNbtReloadForFirstSmoke",
            "terminalItemNetworkInputReachesComputerLikeFirstSmoke",
            "terminalItemNetworkMouseInputReachesComputerLikeFirstSmoke",
            "printerProducesPrintItemAfterEnergyAndInputLikeUpstream",
            "transposerTransfersFluidBetweenAdjacentTanks",
            "microcontrollerBlockStoresComponentsAndStarts",
            "robotAcceptsForgeEnergyCapabilityLikeComputerCase",
            "robotTankUpgradeExposesInternalFluidTank",
            "robotComponentUseRunsSelectedToolThroughFakePlayer",
            "droneItemPlacesRuntimeEntityWithComponentsAndCallbacks",
            "powerConverterAcceptsForgeEnergyCapabilityLikeUpstream",
            "chargerChargesInternalTabletFromStoredEnergyAndRedstoneSpeed",
            "poweredMachineBlocksAcceptForgeEnergyCapabilityLikeUpstream",
            "texturePickerDescribesTargetBlock"
        }) {
            assertTrue(matrix.contains(gameTestName), "Matrix missing concrete GameTest evidence: " + gameTestName);
        }
        for (final String printGameTestName : new String[]{
            "printDataCreatesPrintItemStackLikeUpstreamItemData",
            "printItemTooltipShowsConfiguredDataLikeUpstream",
            "printBlockEntityLoadsStackAndTogglesRedstoneLikeUpstream",
            "printBlockActivatesWithHeldItemLikeUpstream",
            "redstoneActivatedButtonPrintReleasesAfterScheduledTickLikeUpstream",
            "beaconAcceptsConfiguredPrintBaseLikeUpstream",
            "brokenPrintDropsConfiguredPrintStackLikeUpstream",
            "printBlockUsesConfiguredOpacityWhenEnabledLikeUpstream",
            "printBlockRayTraceHitsNearestConfiguredShapeLikeUpstream"
        }) {
            assertTrue(matrix.contains(printGameTestName), "Matrix missing detailed print GameTest evidence: " + printGameTestName);
        }
    }
}
