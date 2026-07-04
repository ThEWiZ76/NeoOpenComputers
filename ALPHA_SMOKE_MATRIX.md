# Alpha Smoke Matrix

This matrix maps first-alpha smoke claims to automated evidence and the remaining manual alpha smoke pass.

Screen world rendering stays frozen unless there is a focused repro, focused failing test, narrow patch, and screenshot evidence.

| Area | Automated evidence | Manual alpha smoke |
| --- | --- | --- |
| OpenOS boot | GameTests cover booting a computer case with OpenOS media and visible terminal output. | Start a tier 1 computer from valid boot media and confirm `/home #` prompt appears. |
| Lua/OpenOS API loading | `BiosResourceTest` covers that Lua BIOS does not provide `require`, OpenOS package setup provides it, raw API globals are moved into `package.loaded`, and `component.<type>` resolves the primary component proxy. | In OpenOS, run a Lua script using `local component = require("component")`, then verify `component.gpu` or another installed primary component works. |
| Computer case storage persistence | GameTests cover computer-case hard-drive state through NBT save/load. | Write a small file to a hard drive, save/reload world, and read it back. |
| Disk-drive floppy persistence | GameTests cover disk-drive and rack disk-drive floppy state through NBT save/load. | Insert OpenOS floppy in disk drive, save/reload world, and confirm media stays loaded. |
| Redstone | Unit and GameTests cover redstone card, computer-case redstone IO, screen redstone, charger signal, print signal, and redstone IO block behavior. | Toggle neighboring redstone and verify signal/component behavior in-game. |
| Modem | Network card, linked card, and modem-message tests cover broadcast, send, distance, wake-message, and signal payload paths. | Send a modem message between two computers in a local world. |
| Inventory | Inventory controller, driver registry, adapter/provider, and transposer coverage exercise item inventory access paths. | Use an inventory-facing component against a chest or compatible inventory. |
| Tank | Tank controller and upgrade tests cover tank API shape and no-tank behavior. | Use a tank-facing component against a compatible tank/fluid handler. |
| Transposer | Transposer registration, renderer, and component tests cover block/entity/component shape. | Move an item or fluid between adjacent inventories/tanks. |
| Microcontrollers | GameTests cover tier 1 microcontroller placement, slot restrictions, required CPU/memory/EEPROM inputs, hard-disk rejection, and starting with a programmed EEPROM. | Assemble a tier 1 microcontroller, install CPU, memory, and programmed EEPROM, then verify it starts and the GUI/manual are usable. |
| Power and charging | GameTests cover battery charge, power converter Forge Energy input, charger Forge Energy input, internal tablet charging, player-equipment charging, powered machine Forge Energy input, and charger redstone speed. | Feed Forge Energy into a power converter or computer case, charge one battery/tablet in a charger, and verify a computer stays powered during a short OpenOS session. |
| Texture picker | GameTests cover texture picker atlas naming for a target block, including `minecraft:block/stone`. | Use a texture picker on a normal block and verify the reported atlas texture name is usable for printer shapes. |
| Printer and print | Printer, print item, placed print, redstone activation, held-item activation, button release, beacon-base, configured drops, opacity, texture fallback, tooltip, ray-trace, and render-model tests cover current print smoke paths. | Create a print item, place it, rotate it, activate it, break it, and confirm the configured shape/data remains. |
| Manual and packaging | Manual link/resource tests, metadata tests, jar packaging tests, API jar tests, contributor docs, changelog, and alpha guide tests cover public artifact readiness. | Install `build/libs/neoopencomputers-0.1.0.jar` in a clean NeoForge 1.21.1 client and open the manual/creative tab. |

Manual alpha smoke must follow `VISUAL_SMOKE_RUNBOOK.md`. Save crash reports, client logs, screenshots, and exact reproduction steps for any mismatch.

Required visual proof files before first alpha handoff:

- `01-openos-prompt.png`
- `02-computer-gui.png`
- `03-screen-after-reload.png`
- `04-creative-tab.png`
- `05-manual.png`
- `06-printer-print.png`

## Automated Evidence Anchors

These are the named GameTests that back the automated side of the matrix. They do not replace the manual visual smoke pass, but they make the pre-alpha evidence traceable.

- OpenOS boot: `computerRunsWithLuaBiosAndOpenOsFloppy`, `tier1ComputerWithNetworkCardBootsOpenOsHardDiskToLiveStyleScreenWall`, `tier1ComputerBootsOpenOsHardDiskWithZeroStoredDriveEnergy`, `tier3ComputerBootsOpenOsFromInternalFloppy`, `tier3ComputerBootsOpenOsToTier3ScreenAndKeyboardTerminal`.
- Lua/OpenOS API loading: `BiosResourceTest.bundledLuaBiosDoesNotProvideOpenOsRequire`, `BiosResourceTest.bundledOpenOsProvidesRequireAndComponentPrimaryConvenience`.
- Storage persistence: `computerCaseStorageStateSurvivesNbtReloadForFirstSmoke`.
- Disk-drive floppy persistence: `diskDriveWritableFloppyStateSurvivesNbtReloadForFirstSmoke`, `rackDiskDriveWritableFloppyStateSurvivesNbtReloadForFirstSmoke`.
- Terminal input: `terminalItemNetworkInputReachesComputerLikeFirstSmoke`, `terminalItemNetworkMouseInputReachesComputerLikeFirstSmoke`.
- Redstone: `redstoneCardUsesComputerLocalSides`, `redstoneCardQueuesInputChangeSignal`, `redstoneWakeThresholdStartsComputer`, `redstoneIoWakeThresholdStartsReachableComputer`, `redstoneIoQueuesInputChangeSignalLikeUpstream`.
- Transposer/tank: `transposerTransfersFluidBetweenAdjacentTanks`, `transposerItemTransferRequiresEnergy`, `transposerTransferFluidRequiresEnergy`, `tankControllerInspectsAdjacentFluidTanks`.
- Microcontrollers: `microcontrollerBlockStoresComponentsAndStarts`.
- Power and charging: `powerConverterAcceptsForgeEnergyCapabilityLikeUpstream`, `chargerAcceptsForgeEnergyCapabilityLikeUpstream`, `chargerChargesInternalTabletFromStoredEnergyAndRedstoneSpeed`, `chargerChargesNearbyPlayerEquipmentLikeUpstream`, `poweredMachineBlocksAcceptForgeEnergyCapabilityLikeUpstream`, `computerCaseAcceptsForgeEnergyCapabilityLikeUpstream`.
- Texture picker: `texturePickerDescribesTargetBlock`.
- Printer/print: `printerProducesPrintItemAfterEnergyAndInputLikeUpstream`, `printDataCreatesPrintItemStackLikeUpstreamItemData`, `printItemPlacesConfiguredPrintLikeUpstream`, `printItemTooltipShowsConfiguredDataLikeUpstream`, `printBlockEntityLoadsStackAndTogglesRedstoneLikeUpstream`, `printBlockActivatesWithHeldItemLikeUpstream`, `printBlockRotatesShapeTowardFacingLikeUpstream`, `printBlockRayTraceHitsNearestConfiguredShapeLikeUpstream`, `printBlockFollowsExternalRedstoneInputLikeUpstream`, `redstoneActivatedButtonPrintReleasesAfterScheduledTickLikeUpstream`, `beaconAcceptsConfiguredPrintBaseLikeUpstream`, `brokenPrintDropsConfiguredPrintStackLikeUpstream`, `printBlockUsesConfiguredOpacityWhenEnabledLikeUpstream`.

## Remaining Manual Proof

The current automated smoke report can prove startup, local-world entry, block placement, and MCP command transport. Human or screenshot-capable tooling still has to prove:

- `/home #` is readable in-world after boot.
- Screen text, GUI text, and item tooltips are visually readable.
- Screen output and keyboard input still look correct after save/reload.
- Creative tab and manual navigation are usable in a clean client profile.
- Microcontroller assembly, GUI, and startup are usable from player view.
- Texture picker returns usable atlas texture names such as `minecraft:block/stone`.
- Printer/print visuals and data behavior look sane from player view, including placement, rotation, activation, drops, tooltip data, beacon-base, opacity, and legacy texture-name use.
