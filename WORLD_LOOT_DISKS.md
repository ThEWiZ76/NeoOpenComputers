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

## Evidence

- A fresh isolated world is seeded before startup by build/finish-port-implementation/prepare-world-loot-probe.py. testWorldLoot requires the existing guarded oc-tests-* world property. Probe files include valid, zero-weight, invalid-count, traversal, bundled-collision and legacy-only cases.
- Corrected baseline world-loot-corrected-red-{build,latest,debug}.log has2 required failures: definitions absent from cycling, and missing weighted sampling. The first world-loot-red run aborted on the fixture's missing-method AssertionError and is not a complete baseline gate; its corrected fixture uses normal GameTest assertions.
- world-loot-parser-red-build.log fails1/10 units on the former malformed-count fallback; the structured-field handling is corrected to upstream semantics.
- Build/all2203 units/all907 mixed tests pass at d5930f5. Tests prove metadata/cycling/readonly binary bytes, same-path content override,1000 seeded selections against the expanded-weight reference, zero-weight visibility, stack ownership, long totals4294967294, exact weighted boundaries, another physical world's root and restored global factories/sampling after unload.
- Legacy baseline world-loot-legacy-red-{build,latest,debug}.log has1 required failure: an existing legacy-only world directory is ignored.
- Final legacy runtime: build/all2203 units/all908 mixed tests and all823 absent tests pass, complete summaries, BUILD SUCCESSFUL and normal terminal exits. Logs world-loot-final-{mixed,absent}-{build,latest,debug}.log. The absent run additionally strengthens world isolation with the real ClientboundUpdateRecipesPacket codec and a separate RecipeManager receiver: its captured external output survives unloading the live world overlay while the live server recipe reverts to its base output.

No real client, Modrinth jar replacement, production operation or OS Computer Use occurred. This is server/codec evidence, not client rendering/TCP/reconnect or a two-JVM world-descriptor acceptance claim.

## Required follow-up

Actual dungeon/treasure-chest generation is STILL missing. The reference README and application.conf describe it, but Loot.scala's legacy ChestGenHooks implementation and registration are commented out; no active randomDisk caller exists in either the sparse Scala tree or the archived upstream Java sources. The new sampler is a functional prerequisite, not a completed chest-generation feature. Implement the modern loot-table connection and verify actual generated chest contents. Preserve that misc.lootProbability is described as an entry WEIGHT (default5, compared with iron10/gold5/diamond3), not an arbitrary percentage. The old commented container list names dungeon, desert pyramid, jungle pyramid and stronghold library; verify modern table/pool semantics before choosing the hook. Do not mark chest loot complete just because the sampler tests pass.

World descriptors are loaded at server startup; live editing/reload/broadcast is not implemented here. Actual client metadata/JEI/crafting/reconnect acceptance, world-descriptor JVM restart/native-program acceptance, large load and broader platform acceptance remain open. Full port goal remains active; beta estimate3-6 concentrated workweeks, low confidence.
