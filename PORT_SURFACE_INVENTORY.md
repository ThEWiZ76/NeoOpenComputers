# Port source surface inventory

Baseline: `7d9eac5584e238a890a388a0f697c27c7391caa4`. Port source: `7d4cef09263e77c1798c8f38792b01941a91443f`.

Lexical declared callback names only. Global name matches do not establish the right host, inheritance, registration, behavior, arguments, energy, events or tests. Missing names are review candidates, not confirmed defects. API path matches do not establish signatures or addon compatibility. Assets, recipes, config and live acceptance remain separate inventories.

The baseline Java API was exported read-only from Git because its sparse working checkout omits Java. The full file/declaration crosswalk is PORT_SURFACE_INVENTORY.json.

| Domain | Upstream source files | Declared callbacks |
| --- | ---: | ---: |
| blocks | 41 | 0 |
| items | 96 | 0 |
| tiles | 59 | 60 |
| components_and_traits | 69 | 313 |
| machine | 25 | 6 |
| integrations | 171 | 123 |

Java API files: 144. Missing matching port paths: 0.

## Callback names requiring source review

| Name not found in annotated port declarations | Upstream declaration(s) |
| --- | --- |
| canBreed | src/main/scala/li/cil/oc/integration/forestry/DriverBeeHouse.java:53 |
| canPlayerAccess | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSecureTile.java:38 |
| cancel | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:518 |
| dropIntoItemInventory | src/main/scala/li/cil/oc/server/component/traits/ItemInventoryControl.scala:20 |
| exportIntoSlot | src/main/scala/li/cil/oc/integration/appeng/DriverExportBus.scala:73 |
| getAccess | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSecureTile.java:45 |
| getAvgPowerInjection | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:196 |
| getAvgPowerUsage | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:200 |
| getBeeBreedingData | src/main/scala/li/cil/oc/integration/forestry/DriverBeeHouse.java:76 |
| getBeeParents | src/main/scala/li/cil/oc/integration/forestry/DriverBeeHouse.java:134 |
| getColors | src/main/scala/li/cil/oc/integration/enderstorage/DriverFrequencyOwner.java:97 |
| getConductionLoss | src/main/scala/li/cil/oc/integration/ic2/DriverEnergyConductor.java:31 |
| getConductorBreakdownEnergy | src/main/scala/li/cil/oc/integration/ic2/DriverEnergyConductor.java:36 |
| getControlDisable | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:31 |
| getControlName | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:47 |
| getControlSetting | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:36 |
| getControlSettingName | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:42 |
| getCpus | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:139 |
| getCraftables | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:152 |
| getDrone | src/main/scala/li/cil/oc/integration/forestry/DriverBeeHouse.java:58 |
| getEnergy | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverEnergyInfo.java:31; src/main/scala/li/cil/oc/integration/ic2/DriverEnergy.java:44 |
| getEnergyDemand | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:220 |
| getEnergyPerTick | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverEnergyInfo.java:36 |
| getExportConfiguration | src/main/scala/li/cil/oc/integration/appeng/DriverExportBus.scala:45 |
| getFluidsInNetwork | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:190 |
| getFrequency | src/main/scala/li/cil/oc/integration/enderstorage/DriverFrequencyOwner.java:47 |
| getFrequencyColors | src/main/scala/li/cil/oc/integration/enderstorage/DriverFrequencyOwner.java:92 |
| getFuel | src/main/scala/li/cil/oc/integration/railcraft/DriverWorldspike.scala:28 |
| getFuelSlotContents | src/main/scala/li/cil/oc/integration/railcraft/DriverWorldspike.scala:47 |
| getGasesInNetwork | src/main/scala/li/cil/oc/integration/ec/NetworkControl.scala:22 |
| getHeat | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:43; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:43; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:53 |
| getIdlePowerUsage | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:204 |
| getImportConfiguration | src/main/scala/li/cil/oc/integration/appeng/DriverImportBus.scala:32 |
| getIndividualOnDisplay | src/main/scala/li/cil/oc/integration/forestry/DriverAnalyzer.scala:32 |
| getInsulationBreakdownEnergy | src/main/scala/li/cil/oc/integration/ic2/DriverEnergyConductor.java:41 |
| getInsulationEnergyAbsorption | src/main/scala/li/cil/oc/integration/ic2/DriverEnergyConductor.java:46 |
| getInterfaceConfiguration | src/main/scala/li/cil/oc/integration/appeng/DriverBlockInterface.scala:38; src/main/scala/li/cil/oc/integration/appeng/DriverPartInterface.scala:52 |
| getItemInventorySize | src/main/scala/li/cil/oc/server/component/traits/ItemInventoryControl.scala:15 |
| getItemStack | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:327 |
| getItemsInNetwork | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:160 |
| getMaxEnergyPerTick | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverEnergyInfo.java:41 |
| getMaxHeat | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:48; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:53; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:63 |
| getMaxSteamPerTick | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSteamInfo.java:37 |
| getMaxStoredPower | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:208 |
| getName | src/main/scala/li/cil/oc/common/tileentity/RobotProxy.scala:136 |
| getOwner | src/main/scala/li/cil/oc/integration/enderstorage/DriverFrequencyOwner.java:87; src/main/scala/li/cil/oc/integration/railcraft/DriverWorldspike.scala:31 |
| getOwnerName | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSecureTile.java:50 |
| getProgress | src/main/scala/li/cil/oc/integration/forestry/DriverAnalyzer.scala:29; src/main/scala/li/cil/oc/integration/ic2/DriverMassFab.java:43 |
| getQueen | src/main/scala/li/cil/oc/integration/forestry/DriverBeeHouse.java:67 |
| getReactorEUOutput | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:58; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:73; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:83 |
| getReactorEnergyOutput | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:53; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:63; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:73 |
| getSinkTier | src/main/scala/li/cil/oc/integration/ic2/DriverEnergy.java:50 |
| getSourceTier | src/main/scala/li/cil/oc/integration/ic2/DriverEnergy.java:56 |
| getSteamPerTick | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSteamInfo.java:32 |
| getStoredPower | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:212 |
| getType | src/main/scala/li/cil/oc/integration/railcraft/DriverWorldspike.scala:38 |
| getWirelessFrequency | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:80 |
| getWirelessInput | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:54 |
| getWirelessOutput | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:60 |
| isCanceled | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:508 |
| isDisabled | src/main/scala/li/cil/oc/integration/railcraft/DriverWorldspike.scala:55 |
| isDone | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:513 |
| isNetworkPowered | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:216 |
| isPowered | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:53 |
| isSideOpen | src/main/scala/li/cil/oc/common/tileentity/Microcontroller.scala:117 |
| isWorking | src/main/scala/li/cil/oc/integration/forestry/DriverAnalyzer.scala:26 |
| lastError | src/main/scala/li/cil/oc/common/tileentity/Microcontroller.scala:113 |
| listAllSpecies | src/main/scala/li/cil/oc/integration/forestry/DriverBeeHouse.java:110 |
| producesEnergy | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:63; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:78; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:93 |
| requesting | src/main/scala/li/cil/oc/integration/appeng/NetworkControl.scala:330 |
| setControlDisable | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:58 |
| setControlSetting | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:64 |
| setExportConfiguration | src/main/scala/li/cil/oc/integration/appeng/DriverExportBus.scala:48 |
| setFrequency | src/main/scala/li/cil/oc/integration/enderstorage/DriverFrequencyOwner.java:57 |
| setImportConfiguration | src/main/scala/li/cil/oc/integration/appeng/DriverImportBus.scala:35 |
| setInterfaceConfiguration | src/main/scala/li/cil/oc/integration/appeng/DriverBlockInterface.scala:46; src/main/scala/li/cil/oc/integration/appeng/DriverPartInterface.scala:55 |
| setName | src/main/scala/li/cil/oc/common/tileentity/RobotProxy.scala:126 |
| setReading | src/main/scala/li/cil/oc/integration/tis3d/SerialInterfaceProviderAdapter.scala:63 |
| setSideOpen | src/main/scala/li/cil/oc/common/tileentity/Microcontroller.scala:123 |
| setWirelessFrequency | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:83 |
| setWirelessOutput | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:63 |
| suckFromItemInventory | src/main/scala/li/cil/oc/server/component/traits/ItemInventoryControl.scala:29 |

## API paths requiring source review

