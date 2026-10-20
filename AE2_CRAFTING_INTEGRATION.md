# AE2 crafting integration

Runtime checkpoint: f36740a717c5b9ee121f51f2ab91aca8ef3bf189. Related: [[AE2 controller integration]], [[../02 plans/AE2 integration]], [[../02 plans/Complete portering]]. Pinned AE2 19.2.18 / MC1.21.1 / NeoForge21.1.234.

## Implemented

- me_controller.getCraftables(filter) returns CraftableValue userdata for known item recipes. Recursive OC description filters use the recipe output quantity, while getItemsInNetwork still reports the actual stored quantity, including zero for craftable-only keys.
- CraftableValue.getItemStack returns the result description with long recipe output size; requesting queries the current requested amount. request(amount=1,prioritizePower=true,cpuName="") validates positive integer quantity and returns CraftingStatusValue. A named CPU must exist; the implementation does not silently fall back to an unrelated CPU. Missing grid/controller and delayed loaded-world lookup return nil with the documented reason.
- Status isDone/isCanceled/cancel preserve computing -> nil,computing; failed -> false,request failed (reason); completed cancellation -> false,job already completed. Modern submit failures use lower-case AE2 error-code names, such as incomplete_plan; unknown named CPU includes no cpu named in its reason.
- AE2 performs calculation asynchronously. The OC server-post-tick handler checks Future.isDone before calling get, so no server thread blocks on an unfinished calculation. The completed future is consumed once and submission happens once on the server thread.
- Each active request owns a non-world AE2 managed node with ICraftingRequester service, no idle cost and the controller's owner ID, connected to the already-loaded target node. This supports AE2's requester/link registration contract. Crafted output is refused by the requester and remains in AE2's normal network-storage insertion path, matching upstream behavior. Actual output storage is tested.
- Active requests remain rooted until terminal, even if the program discards its status value. Requester nodes/futures are released after terminal/failure or server stop. Failures with a submitted link cancel it. Loaded-world retry delay backs off to100ticks and never force-loads the target chunk.
- Saved target includes dimension ID, position, side, expected block and AEItemKey components. Saved craftable includes output quantity. Saved status includes a stable request UUID, settings, node data, AE2 link data and terminal/error state. Multiple same-process restored aliases reuse a canonical live request rather than computing/submitting a second job. Terminal history is not currently a separate world ledger; fresh-process and offline-completion behavior requires the restart gate below.
- Fixed reproduced first-controller-checkpoint defect: store of a craftable-only item copied quantity1 instead of the recipe's output4. Catalogue entries now use recipe output count without giving physical ownership. Existing stored items still use their stored quantity, bounded by ItemStack's integer count representation.

## Verified

Missing getCraftables reproduced in native Lua against a real mounted recipe before implementation (ae2-crafting-red.log). Craftable-only database quantity mismatch separately reproduced before the fix (ae2-crafting-output-count-red.log).

Real controller/drive/item cell/creative energy cell, named1k CPU, pattern provider, molecular assembler and chest fixture:

- Native Lua sees a zero-stored craftable output, filters by recipe output size4 and gets a recipe descriptor. Requests8oak planks on the named CPU with prioritizePower=false; the real assembler produces8planks in ME storage. Completed status is true, canceled false, later cancel rejected with exact completed reason.
- Rejects zero quantity; unknown CPU produces a failure status. Missing birch-log processing ingredients produce incomplete_plan.
- A real processing pattern with no external result remains pending deterministically and can be canceled. This avoids assuming a fast ordinary crafting job will still be running at an arbitrary polling tick.
- Save/load a craftable locator, save a newly computing request, restore two aliases and prove all three share one submitted pending job. Cancellation through one restored alias updates all three.
- After canceled processing settles, network logs plus logs actually sent into the real chest and stored planks conserve exactly64plank-equivalent units. No fixed assertion against a varying split between in-network and external items.
- Craftable-only database entry contains4planks as descriptions and no physical stack.

Build/2140 unit tests pass; all731 required GameTests with AE2+EnderStorage+ProjectRed+CC,729 AE2-only and726 without optional mods pass. Logs: ae2-crafting-mixed-verified.log, ae2-crafting-standalone-verified.log, ae2-crafting-absent-verified.log. Artifact/POM verifier confirms optional AE2 code is not bundled or published as a hard dependency.

## Run

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithAE2 --no-daemon --console=plain
exit $LASTEXITCODE
```

Append -PwithEnderStorage -PwithProjectRed -PwithComputerCraft for the mixed fixture. Use one Gradle process at a time and save latest.log before the next server run.

## Required next gate

This checkpoint does not prove full crafting persistence. Run actual separate prepare/verify JVMs with a native Lua local holding active craftable/status objects. Verify fresh AE2 CPU/requester link rebinding, no duplicate extraction/submission, completion/cancellation and exact conserved resources. Cover completion while the OC host is unloaded and then restored from an older VM snapshot; if AE2 cannot resolve completed links, add only the persistence bookkeeping justified by the reproduction. Also test save during computation, terminal/error reconstruction without a live alias cache, target unload/reload, missing/replaced controller and node cleanup. No fresh-process result is claimed here.

Interfaces/import-export buses/cell converters/tool-spatial hooks/item EnvironmentProviders, full client/multiplayer/protection and broad integration acceptance remain open. No client, Computer Use, Modrinth install or screen movement occurred. Real client acceptance must use screen3 with user coordination. Whole-port beta estimate remains3-6 concentrated workweeks, low confidence, including implementation, bugfixes and real ingame/visual tests.

Verification provenance: the final2140-unit build and729-test standalone run followed the null-message failure fallback and restoration of the filter helper's package-private visibility. The731-test mixed and726-test absent logs precede only those final small changes; no changed foreign-linkage or success/cancel path is inferred from unrun tests.

## Follow-up 2b4e1b752ddeda019c877841b135e82325022f8a

The normal actual-JVM active/terminal/failure and host-offline completion gates now have three-case two-process proof, including100ticks with no OC host before delayed completion. CraftingJournal durably owns active requests and compact outcomes; earlier-heap alias caching alone was insufficient and a reproduced startup-bind regression was fixed. See [[AE2 crafting restart]] for exact PIDs/world/logs, limits and remaining failure/unload/client matrix. The old required-next-gate section above describes the earlier checkpoint.
