# Native volume import

Prerequisite checkpoint 25129d5118e22ec3cdbae0687f9c5755179e90b7, implementing step1 of [[../02 plans/Native storage migration]]. **Native disk drivers, RAID, raw storage and bufferChanges policy are not switched over by this checkpoint.** Full port and backend migration remain in progress. See [[Disk modes and floppy control]] and [[Addon save-directory filesystems]].

## Import protocol

NativeVolumeStore(worldRoot).openManaged(data, capacity, buffered) takes a copy of the existing driver/environment tag. It returns a Mount with relative storageRoot, unchanged nodeAddress (or a generated address only when absent), and an existing direct/buffered FileSystemRegistry filesystem. Callers must persist that binding in fs.oc:storage and use the returned address during later runtime wiring; this helper does not modify the original item/host tag or construct a component environment.

Lowercase UUID addresses use their conventional world/opencomputers/<uuid> directory. Other legacy addresses retain their visible identity while using native-legacy/<SHA-256> below that world root. Saved bindings accept only those two root forms. This avoids path traversal/case aliasing from arbitrary node strings and lets a binding survive later node-address remapping. Managed formatting already removes the fs tag; runtime wiring must preserve that rule for the new binding.

Inline fs.children trees are validated before publication: actual RAM serialization has an implicit root directory, while child entries carry isDirectory; duplicate/invalid names, malformed data/child/root metadata and invalid bindings fail explicitly. Import files are staged outside the mounted tree, flushed, and moved without REPLACE_EXISTING. Existing nonidentical/foreign files or occupied destinations refuse the import. An already-identical complete directory can be adopted without rewriting its files or timestamps. No unrelated files, source NBT or legacy archives are deleted.

Version1 pending/complete compressed NBT journals in opencomputers/.native-imports contain the relative root and a canonical SHA-256 of sorted source paths/types/bytes/timestamps. A pending import may add missing entries only when all existing paths match its original source; a completed import makes disk contents authoritative and never replays stale inline bytes. Journal corruption, a different inline generation, missing bound/completed volumes and missing recovery source fail closed. One JVM lock serializes mount/import setup; normal mounted IO uses the existing backend. Root/path link checks and bounded journal parsing protect the import boundary; this is not a claim of adversarial cross-process or universal junction/filesystem crash safety.

Backend load/save still owns stream restoration. Both direct/buffered tests restore input/output IDs and positions from old metadata without truncation, and new backend snapshots contain metadata rather than children. Label/lock/owner policy remains the native factory's responsibility in the following stage.

## Evidence

- native-volume-red-build.log is the expected missing-class compilation failure for a new API, not an existing-runtime failure count.
- The first ten focused tests pass. A subsequent test uses the actual RAM serializer and reproduces the omitted root isDirectory flag: native-volume-producer-red-build.log fails1 test. Root parsing is corrected to accept the real implicit directory, and malformed top-level metadata also fails explicitly.
- native-volume-fixed-build.log passes all12 focused tests: nested binary import/empty dirs/mtime, actual RAM producer, source-tag preservation, direct/buffered input/output restore, stale snapshot/disk authority, conflicts and different generations, identical adoption/no timestamp rewrite, pending recovery/refusal, malformed/duplicate trees, unsafe legacy identities/remapped bindings, new/existing external volumes, corrupt journals and missing-bound/pending-source cases. IDs and timestamps are fixed in fixtures.
- Final build/all2218 units/all922 mixed GameTests pass with complete summary, BUILD SUCCESSFUL and normal exit. Logs native-volume-final-seeded-mixed-{build,latest,debug}. The first broad run native-volume-final-mixed mistakenly omitted its external loot seeder and fails5 fixture checks; archived native-volume-unseeded latest/debug logs identify that setup error. The successful run uses a fresh preseeded oc-tests-native-volume-engine-20261020-02 world.

These are real temporary-filesystem unit tests plus a broad non-regression gate. They are not a native-driver import, two-JVM import-interruption or actual-client acceptance claim. No Modrinth/client, Computer Use, production or existing user-world mutation occurred. Power-loss/whole-host snapshot atomicity, mount concurrency after setup, load/platform and wider recovery acceptance remain open.

## Next

Before native runtime wiring, restore the buffered case policy confirmed in upstream FileSystem.scala/Buffered.scala: host case-insensitivity and filesystem.forceCaseInsensitiveFS (default false), with API fromMemory staying case-sensitive. Then wire managed native factories plus bufferChanges=true, RAID binding and external compressed raw storage according to [[../02 plans/Native storage migration]], preserving existing bytes/identities/handles. Full port goal remains active; beta estimate3-6 concentrated workweeks, low confidence. Cosmetic polish remains deferred.
