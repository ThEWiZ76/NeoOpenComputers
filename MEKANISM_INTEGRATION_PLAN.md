# Mekanism integration plan

Related: [[Complete portering]], [[../04 ai-context/Modern integration scope]]. Execute within feature/finish-port; intermediate commits/pushes authorized. No client installation or desktop interaction in this work package.

## Contract and design

Pinned target: Mekanism 1.21.1-10.7.19.85, verified in publisher-linked ModMaven metadata and exact API/source artifacts on 2026-10-20. Official setup: https://github.com/mekanism/Mekanism/tree/1.21.x . Use an optional compile dependency and opt-in withMekanism runtime; no bundled foreign classes or mandatory published dependency.

The four upstream integration files register a gas converter and Mekanism wrench checks/usage, not a machine component driver. Modern ChemicalStack replaces GasStack. Preserve amount/name/label/debug-ID fields with exact long quantities and registered modern chemical names; explicitly test normalized empty stacks. IMekWrench was replaced by Mekanism item abilities. Recognize configurator modes as wrench tools while allowing block wrench actions only with the WRENCH_DISMANTLE ability, equivalent to old WRENCH mode. Preserve drone pickup's upstream type-only check. Do not invent getGasesInNetwork here: that is ExtraCells scope.

Generic FE item charging is also missing from the port. Restore it as a NeoForge fallback in the default item-charge provider after OC Chargeable handling. Charge methods return internal-energy surplus, simulate without mutation, and account only for actual received FE. The old direct Mekanism Joule power trait is commented out in the reference baseline; verify actual modern native FE block/item interoperability instead of claiming a nonexistent active legacy trait has been ported.

## Steps

- [x] Add pinned optional dependency/profile and server guard. Add real Mekanism tests and save missing-converter/wrench/charging evidence before implementation.
- [x] Register ChemicalStack conversion, covering real gas and non-gas modern chemicals, large counts, nested conversion, optional registry IDs and empty normalization without stack mutation.
- [x] Register modern wrench checks/usage. Restore adapter/charger holdsApplicableWrench checks and successful charger wrenchUsed notification. Test all configurator modes, wrong-mode no mutation, main/offhand behavior, survival adapter/charger use and unchanged tools; keep drone type-only pickup semantics. Existing AE2/ProjectRed/core wrench regression checks must stay green.
- [x] Restore generic FE item charging with real energy tablet and OC charger tests: exact source/destination energy, partial/full acceptance, simulation, no charge for non-energy items, existing OC provider priority. Verify native Mekanism energy-cube/cable to OC FE input behavior with real blocks and document exact test scope.
- [x] Add artifact/POM optional-isolation guard, run units/build, minimal Mekanism, mixed-mod and absent profiles. Use one Gradle process at a time and preserve logs. Canonical documentation first, repository mirrors, commits/pushes and remote equality verification.

## Remaining acceptance

Client menus/models/tool interaction, actual survival play, multiplayer/protection, broader restart/platform/load and user configuration variants remain part of full-port acceptance. Minecraft must use screen 3 with desktop coordination; no obfuscated PowerShell or antivirus bypass. This package does not close ExtraCells or the whole integration inventory.

Whole-port beta estimate remains 3-6 concentrated workweeks, low confidence, including full implementation, bugfixes and real-client acceptance. Re-estimate after verified milestones; test counts are not implementation percentages.

Mekanism checkpoint 8e852cb544df38e7bba0becd166a4e6ab64d0465: optional chemical descriptions/native configurator ability mapping, exact long/native53 proof, generic FE item charging and original OC wrench eligibility/notification restored. Adapter/charger honor usability; ProjectRed charger usage consumes once; drone keeps upstream type-only pickup. Seven new Mekanism tests plus one core test and artifact/POM guard; build/2142 units/773 mixed/734 Mekanism-only/727 absent pass in isolated worlds with clean exit. Native cube/cable transfers1000FE to OC. Reused-world shutdown unload loop and discarded premature world-lock run documented, not claimed fixed. See [[../03 manuals/Mekanism integration and FE item charging]]. Client/multiplayer/load and other integration/semantic gates remain open; beta3-6 concentrated workweeks, low confidence. No client/Computer Use action.
