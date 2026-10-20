# World loot disks

Functional checkpoints d5930f5b8fd9beae9bf0e19204bc915c8edf5b25 and b4d462200f10eb63fc7281fc48bb8d0df4a5a8bd. See [[Addon save-directory filesystems]], [[Loot disk cycling]] and [[Loot disk metadata synchronization]].

## World content and lifecycle

Server startup reads <save-root>/opencomputers/loot/loot.properties. Each key identifies a relative content folder below that loot directory. Java Properties input/escaping follows upstream. Values use folder=label:weight:dyeOreName, or label:weight, or a plain label. Exactly2/3 fields have structured meaning; other field counts remain the complete legacy label with weight1. Invalid numeric weights and unsafe/absolute/traversing paths are skipped with a warning. Negative weights act as zero; unknown colors fall back to light gray.

For example, example=Example Program:2:dyeBlue maps to opencomputers/loot/example/. Files are mounted read-only on demand. A zero weight disables random sampling only: the disk still appears in login metadata and remains a cycling output. No filesystems are evaluated merely to construct metadata or sampling templates.

WorldLootDisks keeps immutable definition snapshots per MinecraftServer and original ItemRegistry identity. AboutToStart loads them; ServerStopped removes them. World factories do not overwrite permanent addon factory/template maps. Registry lookups consult the active world's overrides first; stopping/unloading restores the original bundled/addon binding. Each world factory captures its physical world root, so reading another world's descriptors cannot redirect an existing factory. A different temporary API registry does not inherit the original registry's world overlay.

Cycling and login metadata merge by factory ID: a world definition at a bundled path selects the world's files and display template while keeping one ID, and new IDs append in descriptor order. The standard authoritative recipe stream also captures the world outputs. Client display state remains separate from the integrated-server item registry; no new custom wire format is added here.

## Weighted selection

ModLootDisks.randomDisk(RandomSource) samples copied count1 stacks for the active server. World descriptor entries are followed by bundled descriptor entries, preserving the upstream additive sampling model. A same-path world definition overrides the factory/content but does not subtract the bundled sampling entry or its weight; sampled variants can retain their respective labels/colors. Addon registrations without descriptor weights are not added to this pool. No active world/pool returns ItemStack.EMPTY.

LootDiskPool stores weighted entries with a long total rather than expanding one stack reference per weight unit. Ordinary totals use nextInt(total), matching the legacy sampling model; totals above Integer.MAX_VALUE use the JDK bounded-long sampler. Zero weights are omitted from random selection, and caller mutations cannot alter private templates.

## Legacy oc:lootPath disks

The port previously looked only in bundled resources. It now checks an existing world loot directory first and otherwise retains bundled fallback, including existing OpenOS compatibility. It preserves the legacy filesystem label and exposes world loot content read-only. Upstream DriverLootDisk had a writable filesystem type constrained indirectly by zero capacity; the port deliberately uses an explicit read-only wrapper so a write-open cannot truncate world content. Invalid legacy paths fail without escaping the content root. No old files are moved or rewritten.

## Actual treasure-chest generation — e4bf5dceb9466e71b58591baebd180d8d5af8ac0

ChestLoot registers the serializable neoopencomputers:loot_disk singleton entry. Its selected output comes from the active world sampler, retaining each disk's factory, label and color. misc.lootProbability is an entry weight, default5, range0..10000; zero disables injection on the next server resource reload. It is not a percentage. Generated/opened chest inventories are not retroactively changed.

The four targets are minecraft:chests/simple_dungeon, desert_pyramid, jungle_temple and stronghold_library. NeoForge's load event injects exactly one entry into the pool containing iron_ingot, or book for the library, recursively checking composite children. Current vanilla dungeon uses its second pool; the other three use the first. A datapack without those anchors uses its first existing pool; an entirely pool-free table remains empty. Existing direct/composite disk entries prevent duplicate injection. Datapacks may explicitly use the registered entry with standard weight/quality/condition/function fields.

Targeted mixin accessors preserve the existing table and pool identities, order, registry holders, names, conditions, functions, entries, roll counts and random sequences. The only replacement is the selected pool's entry list, before table/pool freezing. A post-generation additive GLM would not preserve weighted competition with original entries. The first whole-table-codec approach failed during real startup because unresolved enchantment tag owners cannot be re-encoded in that load context; it was removed. No reflection or wholesale vanilla JSON replacement is used.

Evidence: chest-loot-red logs reproduce two required failures (no weighted entry and no chest disks). Final build/all2203 units/all911 mixed and all826 absent GameTests pass with complete summaries and normal process exits. Final logs are chest-loot-final-mixed-02 and chest-loot-final-absent, with build/latest/debug variants. Both final worlds contain the guarded external descriptor fixtures. Tests perform512 deterministic lazy generations through a real ChestBlockEntity, across all four vanilla tables, proving vanilla loot and registered disks coexist and each target can select a world-defined disk. A separate fixture tests weight7, disable0, idempotence, empty/unrelated tables and exact preservation of pool/table data. Loaded mixed tables additionally retain ComputerCraft's treasure-disk pool.

The first mixed preservation fixture posted the global event and incorrectly blamed ComputerCraft's valid additional pool on OC. The diagnostic log identifies computercraft:treasure_disk. The corrected preservation fixture invokes OC's handler alone; startup-loaded tables separately prove actual event registration and coexistence. These historical failed attempts are not counted as passing gates.

No real client, OS Computer Use, Modrinth installation or production operation occurred. This proves server-side loaded-table and lazy inventory generation, not discovery/opening of terrain-generated structures by a live player. Beta remains3-6 concentrated workweeks with low confidence; functional work precedes cosmetic polish.

## Native world/legacy disk restart — 3bc5314ab06b1408e03a0a36defe07042be01f69

WorldLootProcessRestartGameTests adds opt-in restartWorldLoot through the existing guarded restartPhase/restartWorld profile. tools/prepare_world_loot_restart.py seeds only an absent oc-restart-world-loot-* world, checks its resolved parent, and preserves existing data. Its network descriptor deliberately overrides a bundled factory path; a separate legacy-only directory has no bundled fallback. The ordinary world-loot fixture flag is not required for this restart profile.

In oc-restart-world-loot-20261020-02, prepare PID58848 and fresh-JVM verify PID72588 each pass all827 required tests and exit normally. Build/all2203 units pass. Final logs world-loot-final-restart-{prepare,verify}-{build,latest,debug}. Earlier pair -01 also passes in PIDs12788/72224; -02 adds a waiting-marker precondition so an already-resumed VM cannot produce a false positive on verify reuse. Both worlds are already verified and must not be reused as fresh preparation.

Preparation obtains the actual world-defined disk by lazy dungeon-chest generation, installs it and an oc:lootPath legacy disk in two physical disk drives beside a native Lua5.3 computer, and leaves one read handle open on each at offset1. Normal server shutdown persists the live machine and hosts. The new process reloads world definitions before resolving those saved disks. It preserves factory/label/color fields, item/count, computer/drive addresses, retained Lua locals and each filesystem's identity. The resumed program checks both offsets and reads exact remaining binary bytes (00 80 ff 2a and 00 ff), then closes the handles. Write-open attempts are rejected; independent Java file reads verify the original source bytes remain61 00 80 ff 2a and11 00 ff. Stack driver state may legitimately gain persistent node/handle metadata, so descriptor checks compare its stable fields rather than requiring an unused template's entire NBT to equal a saved disk.

Reproduce with an unused name and sequential commands from the repository root:

```powershell
rtk proxy python tools/prepare_world_loot_restart.py oc-restart-world-loot-local-01
rtk proxy cmd /d /c "gradlew.bat --no-daemon runGameTestServer -Pneoopencomputers.restartWorldLoot -Pneoopencomputers.restartPhase=prepare -Pneoopencomputers.restartWorld=oc-restart-world-loot-local-01"
rtk proxy cmd /d /c "gradlew.bat --no-daemon runGameTestServer -Pneoopencomputers.restartWorldLoot -Pneoopencomputers.restartPhase=verify -Pneoopencomputers.restartWorld=oc-restart-world-loot-local-01"
```

This closes targeted case/two-drive native5.3 world/legacy handle acceptance on Windows. No runtime fix was needed. It does not prove every host/architecture/platform, descriptor mutation between processes, live player/TCP/reconnect/structure discovery or crash-mid-save behavior. No client, OS Computer Use, Modrinth install or production operation occurred. Technical parity remains the priority.

## Evidence

- A fresh isolated world is seeded before startup by build/finish-port-implementation/prepare-world-loot-probe.py. testWorldLoot requires the existing guarded oc-tests-* world property. Probe files include valid, zero-weight, invalid-count, traversal, bundled-collision and legacy-only cases.
- Corrected baseline world-loot-corrected-red-{build,latest,debug}.log has2 required failures: definitions absent from cycling, and missing weighted sampling. The first world-loot-red run aborted on the fixture's missing-method AssertionError and is not a complete baseline gate; its corrected fixture uses normal GameTest assertions.
- world-loot-parser-red-build.log fails1/10 units on the former malformed-count fallback; the structured-field handling is corrected to upstream semantics.
- Build/all2203 units/all907 mixed tests pass at d5930f5. Tests prove metadata/cycling/readonly binary bytes, same-path content override,1000 seeded selections against the expanded-weight reference, zero-weight visibility, stack ownership, long totals4294967294, exact weighted boundaries, another physical world's root and restored global factories/sampling after unload.
- Legacy baseline world-loot-legacy-red-{build,latest,debug}.log has1 required failure: an existing legacy-only world directory is ignored.
- Final legacy runtime: build/all2203 units/all908 mixed tests and all823 absent tests pass, complete summaries, BUILD SUCCESSFUL and normal terminal exits. Logs world-loot-final-{mixed,absent}-{build,latest,debug}.log. The absent run additionally strengthens world isolation with the real ClientboundUpdateRecipesPacket codec and a separate RecipeManager receiver: its captured external output survives unloading the live world overlay while the live server recipe reverts to its base output.

No real client, Modrinth jar replacement, production operation or OS Computer Use occurred. That earlier checkpoint was server/codec evidence only; the later native restart checkpoint above supplies the targeted two-JVM proof, while client rendering/TCP/reconnect remains open.

## Required follow-up

Chest generation is implemented at the checkpoint below. Live client/structure discovery and broader compatibility remain open; targeted native world/legacy disk restart proof is recorded above. The full port is not complete.

World descriptors are loaded at server startup; live editing/reload/broadcast is not implemented here. Actual client metadata/JEI/crafting/reconnect acceptance, wider native host/architecture/load and broader platform acceptance remain open; the case/two-drive native5.3 world-descriptor restart is now proven above. Full port goal remains active; beta estimate3-6 concentrated workweeks, low confidence.


## Shared read-only handle regression — 57b2250819f0a59b8a1ea2639c5fdb6c34c8db02

[[Managed disk locking]] records the later shared-wrapper fix and expands the native restart fixture with a third, internal locked managed HDD. The earlier two-drive checkpoint remains historical proof; current restartWorldLoot runs also assert3 open handles, persisted HDD lock owner and write denial after JVM restart. New world oc-restart-world-loot-managed-lock-20261020-01: PIDs59468/64684, both831 tests with normal exits; build2204 units and915 mixed tests pass. No native backend migration or client/CU action.
