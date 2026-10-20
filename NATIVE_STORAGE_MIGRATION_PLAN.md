# Native storage migration implementation plan

> Execute with superpowers:executing-plans on the existing feature/finish-port branch. The user already authorized the complete plan and implementation. No new approval loop, worktree replacement, plan-review subagent or production operation.

**Goal:** Restore native managed HDD/floppy/RAID world-backed storage and bufferChanges, plus external compressed raw-drive storage, while preserving current NBT-backed data, labels, identities and open streams.

**Architecture:** Use the existing direct/buffered save-directory filesystems. A small NativeVolumeStore imports legacy inline managed trees without mutating their source, records an import journal outside the mounted directory, and returns a mount with an explicit relative storage binding. That binding is independent of later network-address remapping. Native factories and RAID will persist it in fs metadata; raw storage gets the corresponding explicit binding and binary import. No global RAM fallback merely to satisfy serverless tests.

**Tech stack:** Java21, NeoForge21.1.234/MC1.21.1, NBT, NIO, existing BufferedFileSystem IO queue, JUnit/GameTests/native Lua5.3 restart probes. Canonical notes in Obsidian, repository mirrors and intermediate feature pushes.

## Invariants

- Existing inline bytes remain available until a complete, durable import exists. A conflict never overwrites or deletes either source. Import/open failure must remain explicit; no silent empty replacement.
- Existing completed imports make on-disk data authoritative, so reopening a stale old NBT snapshot cannot roll files back. Pending imports may resume only when every already-present entry matches the expected snapshot and there are no foreign entries.
- Existing unrelated directories, files, occupied paths, legacy archives and addon save roots remain unchanged. Invalid paths, duplicate names, malformed trees and unsafe storage bindings fail before publishing an import.
- UUID node addresses keep their conventional root when possible. Non-UUID/unsafe legacy addresses use a separate hashed namespace; the visible component identity is preserved. A saved storage binding must survive network node remapping; a real format/mode transition must discard the old binding.
- File contents move out of new host/item NBT; stream IDs/positions, ownership, labels, configuration and node identity remain. Locked content imports before applying readonly protection.
- Preserve the upstream bufferChanges default true. Direct mode and buffered mode both work. No implicit migration of /tmp, RAM addon filesystems or loot content.
- No general crash-proof heap/filesystem transaction claim: normal save/restart, import interruption/recovery and IO failures require separate evidence. Current direct/buffered API guarantees and outstanding crash/load/platform gates remain explicit.

## Execution

### 1. Guarded managed import engine

Files: create common/NativeVolumeStore.java and src/test/java/li/cil/oc/common/NativeVolumeStoreTest.java.

- [x] Add tests for binary/nested import, preserved source NBT, direct/buffered reopen, saved input/output positions, existing identical trees, foreign conflicts, resumable partial imports, stale-snapshot replay, malformed paths/trees and preserved node identity/bindings.
- [x] Run `rtk proxy cmd /d /c "gradlew.bat --no-daemon test --tests li.cil.oc.common.NativeVolumeStoreTest"` and record the failing behavior/contract before production implementation.
- [x] Implement a fresh-world-root service, deterministic identity injection for tests, validated relative roots, canonical tree digest and versioned pending/complete NBT journals in .native-imports outside the mounted tree. Use CREATE_NEW for imported files; verify existing bytes/topology before continuation. Complete the journal before returning a usable mount. Reuse FileSystemRegistry for actual direct/buffered behavior.
- [x] Green focused units, preserve logs and commit/push this prerequisite without claiming native runtime wiring complete.

### 2. Native HDD/floppy wiring and compatibility

Files: DiskItemEnvironment, HardDiskDriveItem, FloppyItem, ModSettings; native storage GameTests and existing unit fixtures.

- [ ] Reproduce current native payload staying in NBT and missing world files with actual disk drivers/hosts.
- [x] Restore buffered filesystem case policy before native wiring: upstream FileSystem.scala wraps BufferedFileSystem.segments with toCaseInsensitive when the actual filesystem is insensitive or filesystem.forceCaseInsensitiveFS=true (default false). Current BufferedFileSystem/MemoryFileSystem is case-sensitive. Test a forced policy deterministically, host-default lookup, creation/rename/open/load and duplicate-case legacy trees without silent merge or data loss. Keep API fromMemory case-sensitive.
- [ ] Add filesystem.bufferChanges (default true); bind managed mounts to the active world and persist fs.oc:storage. Preserve old node identities and handle/owner metadata while importing inline data before old payload is omitted from future snapshots. No empty out-of-world driver substitution.
- [ ] Replace serverless native-storage assumptions with explicit unit dependencies or actual world GameTests; retain their behavioral assertions. API fromMemory stays RAM.
- [ ] Test direct/buffered modes, locks, legacy data, capacity changes, item removal/copy/menu flows, current mode/no-op versus format, and address remapping without lost data. Commit/push verified integration.

### 3. RAID binding

Files: RaidBlockEntity and RAID/native process probes.

- [ ] Reproduce array payload staying in NBT; migrate the array's old tree with its preserved filesystem identity and explicit binding.
- [ ] Keep destructive member formatting/removal semantics. Do not erase array data merely because a chunk/network reload reconstructs a mount. Verify reassembly creates a new empty volume and open-handle cleanup.
- [ ] Test regular reload and a real native JVM restart; commit/push.

### 4. Raw-drive external storage

Files: DriveEnvironment, native storage helper and raw-drive/mode tests.

- [ ] Reproduce inline raw bytes. Import old bytes to an external compressed volume without clobbering an existing file; persist a stable binding independently of node remapping.
- [ ] Keep signed readByte, capacity/platter/timing, label/lock and mode-format semantics; preserve inline source until durable import. Invalid/compressed/conflicting existing files must fail explicitly.
- [ ] Test binary IO, lock, direct save/reload, clone/conflict behavior, normal two-JVM native restart and recovery cases; commit/push.

### 5. Complete acceptance and documentation

- [ ] Run build/all units, a real optional-provider mixed GameTest profile and provider-absent/native restart profiles, sequential Gradle only. Check complete required-test summaries and terminal exit0; archive logs before the next run.
- [ ] Coordinate a bundled actual-client update and functional tests on screen3 when needed; headless evidence is not client rendering/TCP/Survival acceptance.
- [ ] Update canonical manuals/plan/project memory first, read back, then mirrors and lexical inventory. Verify local/remote feature SHA equality and clean state after pushes. No merge, publication or production restart.

Full port scope remains active. Cosmetic polish remains deferred. Beta estimate remains3-6 concentrated workweeks with low confidence, to be revised from remaining technical scope and acceptance evidence rather than test counts.


## Step1 checkpoint — 25129d5118e22ec3cdbae0687f9c5755179e90b7

[[../03 manuals/Native volume import]] records12 focused tests, build2218 units/922 mixed and complete normal exits. This closes the guarded import prerequisite only. Native runtime wiring, buffered case policy/configuration, RAID/raw storage and their actual process/client acceptance remain open; no migration of user disks occurred.

Case-policy checkpoint: runtime `3babca34672ca4caae7c1c3a91e3188433282404`, full build2223 units/923 mixed required GameTests passes with normal exit. Host-default and forced config covered; API RAM unchanged, saved streams and collision preservation covered. See [[../03 manuals/Addon save-directory filesystems]]. Native factory/config bufferChanges, RAID and raw wiring remain open.
