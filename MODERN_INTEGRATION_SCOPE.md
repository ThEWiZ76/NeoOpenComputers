# Modern integration scope - 2026-10-20

Related: [[../Project]], [[../02 plans/Complete portering]], [[Port gap triage]], [[Port parity matrix]], [[PORT_SURFACE_INVENTORY]].

Baseline7d9eac5584e238a890a388a0f697c27c7391caa4 contains171 integration source files in21 directory groups plus root metadata. The groups include core OC, Minecraft and shared helpers: this is not171 mods. Callback counts below are lexical declarations, excluding inheritance/dynamic peripherals/events/converters; zero does not mean no behavior. Counts are scope evidence, never implementation percentages.

| Source group | Files | Declared callbacks | Current decision / remaining work |
| --- | ---: | ---: | --- |
| (root metadata) | 4 | 0 | Bootstrap/mod-proxy metadata, not a mod family; modern activation/blacklist parity still to audit. |
| appeng | 12 | 27 | Controller11 network/storage/CPU/energy callbacks implemented at5b60fee0b with real server/native and mixed/standalone/absence proof. Crafting userdata/async submission/real output/cancel and same-process alias reload implemented atf36740a71; normal three-case fresh-process/host-offline persistence now proved at2b4e1b752 with world journal and startup rebind; block/panel interfaces, import/export config/providers and3-host CPU busy/too-small/absent matrix implemented atb40e4be50; manual exportIntoSlot implemented with real sided/native/conservation proof (AE2_MANUAL_EXPORT.md); computing-save/true chunk eviction/unload-loop stability, remaining failure matrix, converters/auxiliary hooks and client acceptance still open. See AE2_INTERFACE_CONFIGURATION.md. See AE2_CRAFTING_RESTART.md. See AE2_CRAFTING_INTEGRATION.md. See AE2_CONTROLLER_INTEGRATION.md and AE2_INTEGRATION_PLAN.md. |
| charset | 1 | 0 | Bundled-redstone bridge. Modern provider/version or explicit replacement decision still unverified. |
| cofh | 7 | 15 | Energy/redstone/security/steam and item-charge APIs. Modern CoFH/Thermal equivalents still to verify; generic FE is not all these callbacks. |
| computercraft | 9 | 0 | Bundled transport already tested. Peripheral/media/Lua-object/relay-specific contracts still need comparison; zero annotated callbacks does not mean no integration API. |
| ec | 5 | 1 | ExtraCells AE2 extensions. Modern replacement and API scope still unverified; do not count as core AE2 parity. |
| enderio | 2 | 0 | Power/bundled event hooks; modern provider and distinct behavior still to verify. |
| enderstorage | 2 | 5 | Five adapter callbacks implemented at820fee37b; real server/native, standalone/mixed/absent profiles pass. Client/protection/restart/load acceptance remains. |
| forestry | 6 | 9 | Bee/analyzer/genetics APIs and converters. getDrone means the bee-house callback, not an OC drone/debug-card method. Modern provider/version still to verify. |
| ic2 | 10 | 24 | Reactor/mass-fabricator/energy/conductor callbacks and electric-item support. Modern API/replacement choice still unverified. |
| jei | 7 | 0 | Official1.21/1.21.1 NeoForge setup verified. Manual/callback docs/loot cycling/GUI integration remains open. |
| mekanism | 4 | 0 | Power/item charging plus gas conversion. Modern chemical APIs require explicit mapping and tests; FE/fluid capability support is not complete parity. |
| minecraft | 22 | 30 | Vanilla drivers/converters exist; exact callback/config/resource/player acceptance remains in the main parity matrix. |
| minecraftforge | 3 | 4 | Modern NeoForge capabilities used. Old power/events/config behavior requires contract-level review, not only interface substitution. |
| opencomputers | 56 | 0 | Core OC items/upgrades/providers, not an external-mod count. Existing implementations/tests remain subject to host/API semantic audit. |
| projectred | 2 | 0 | Bundled16-channel and screwdriver integration has documented server proof. Client/other electrical tools and broader acceptance remain. |
| railcraft | 3 | 5 | Worldspike and entity-event integration. Modern Railcraft provider/API or replacement still to verify. |
| thaumcraft | 2 | 0 | Item conversion/integration hook. Modern provider or explicit non-applicability decision still unverified. |
| tis3d | 2 | 3 | Adapter serial-interface bridge. Modern version/API and real serial acceptance still to verify. |
| util | 8 | 0 | Shared cross-mod power/charging/wrench/redstone/search helpers, not an external mod. Provider registration and compatibility policy still to audit. |
| waila | 2 | 0 | Overlay data provider. Modern WAILA/WTHIT/Jade mapping and live visual acceptance still unverified. |
| wrcbe | 2 | 0 | Wireless-redstone provider. No positive modern version verified yet; a failed project-slug lookup is not proof no compatible provider exists. |

## Availability evidence checked this checkpoint

- AE2: publisher-linked Modrinth API lists19.2.18 as a1.21.1 NeoForge release. Its official [1.21.1 branch/API guidance](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/1.21.1) provides the Maven Central API artifact. This proves an implementation target exists, not OC integration.
- JEI: the maintainer's [1.21/1.21.1 setup](https://github.com/mezz/JustEnoughItems/wiki/Getting-Started-%5BMinecraft-1.21-and-1.21.1%5D) documents NeoForge/NeoGradle API and runtime artifacts. Publisher-linked Modrinth metadata lists stable19.64.0.455; no JEI dependency was added in this checkpoint.
- EnderStorage: official1.21.1 Git branch and published1.21.1-2.13.0.190/.191 Maven artifacts verified. Use [[../03 manuals/EnderStorage integration]] for the implemented five-callback contract and exact dependencies/proof. A similarly named Modrinth project without a source link was not used as authoritative evidence of this mod's absence.
- Existing ProjectRed/CC versions and proof remain in their manuals. All other availability entries above are deliberately unverified until checked against primary provider sources. No legacy integration was silently dropped or declared non-applicable.

Read-only evidence snapshots and pinned source extracts live under build/finish-port-implementation/integration-discovery/. Network discovery used public GitHub APIs/raw source, publisher Maven metadata and publisher-linked Modrinth metadata; no credentials were used. Public APIs can change, so the dated versions are pinned targets, not perpetual latest-version claims.

## Next implementation order

1. AE2 adapter network/storage/crafting contracts, including real powered-grid tests; then interface/import-export and converter behavior. Keep async crafting state, callbacks, arguments/results/errors, energy and owner/security behavior explicit.
2. Complete the CC peripheral/media/relay contract audit and modern chemical/power/item-charging mappings.
3. JEI/manual/tooltip integrations with actual client acceptance, and each remaining provider family or an explicit compatibility/replacement decision backed by primary sources.
4. Supported minimal/mixed profiles, optional-classloading/packaging, save/unload/restart, multiplayer/protection and long-load acceptance. Client work must respect the screen3 constraint.

Beta remains3-6 concentrated workweeks with low confidence for full implementation plus acceptance. The integration surface is still open; the EnderStorage checkpoint does not close the broad modern-integration gate. No client or Computer Use activity occurred here.

Export checkpoint 37fb64458a819bd3e45377174a1cabdf69e7bf41: actual exportIntoSlot with sided inventory/slot handling, speed/fuzzy filters, powered machine extraction, rejected-remainder refund/drop and exact item conservation. Ten new server/native tests; build/2141 units/746 mixed/726 absent pass. See [[../03 manuals/AE2 manual export]]. Converter/auxiliary, remaining crafting/stability/client gates stay open; beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.
