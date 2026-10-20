# Port source surface inventory

Baseline: `7d9eac5584e238a890a388a0f697c27c7391caa4`. Port source: `805cebb67a981009dcb0f57ec3b1968244d1c6e4`.

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
| canPlayerAccess | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSecureTile.java:38 |
| getAccess | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSecureTile.java:45 |
| getConductionLoss | src/main/scala/li/cil/oc/integration/ic2/DriverEnergyConductor.java:31 |
| getConductorBreakdownEnergy | src/main/scala/li/cil/oc/integration/ic2/DriverEnergyConductor.java:36 |
| getControlDisable | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:31 |
| getControlName | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:47 |
| getControlSetting | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:36 |
| getControlSettingName | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:42 |
| getEnergy | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverEnergyInfo.java:31; src/main/scala/li/cil/oc/integration/ic2/DriverEnergy.java:44 |
| getEnergyPerTick | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverEnergyInfo.java:36 |
| getGasesInNetwork | src/main/scala/li/cil/oc/integration/ec/NetworkControl.scala:22 |
| getHeat | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:43; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:43; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:53 |
| getInsulationBreakdownEnergy | src/main/scala/li/cil/oc/integration/ic2/DriverEnergyConductor.java:41 |
| getInsulationEnergyAbsorption | src/main/scala/li/cil/oc/integration/ic2/DriverEnergyConductor.java:46 |
| getMaxEnergyPerTick | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverEnergyInfo.java:41 |
| getMaxHeat | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:48; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:53; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:63 |
| getMaxSteamPerTick | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSteamInfo.java:37 |
| getOwnerName | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSecureTile.java:50 |
| getReactorEUOutput | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:58; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:73; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:83 |
| getReactorEnergyOutput | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:53; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:63; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:73 |
| getSinkTier | src/main/scala/li/cil/oc/integration/ic2/DriverEnergy.java:50 |
| getSourceTier | src/main/scala/li/cil/oc/integration/ic2/DriverEnergy.java:56 |
| getSteamPerTick | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverSteamInfo.java:32 |
| getWirelessFrequency | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:80 |
| getWirelessInput | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:54 |
| getWirelessOutput | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:60 |
| isPowered | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:53 |
| producesEnergy | src/main/scala/li/cil/oc/integration/ic2/DriverReactor.java:63; src/main/scala/li/cil/oc/integration/ic2/DriverReactorChamber.java:78; src/main/scala/li/cil/oc/integration/ic2/DriverReactorRedstonePort.java:93 |
| setControlDisable | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:58 |
| setControlSetting | src/main/scala/li/cil/oc/integration/cofh/tileentity/DriverRedstoneControl.java:64 |
| setWirelessFrequency | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:83 |
| setWirelessOutput | src/main/scala/li/cil/oc/server/component/RedstoneWireless.scala:63 |

## API paths requiring source review

