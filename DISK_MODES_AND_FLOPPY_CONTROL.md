# Disk modes and floppy control

Runtime ce9d912a2196a1ffa30966b0316a480ee0eb358f. See [[Managed disk locking]], [[World loot disks]] and [[../02 plans/Complete portering]]. Technical functionality; existing DriveScreen presentation is reused.

## Functional contract

Upstream FloppyDisk inherits FileSystemLike, whose ordinary non-sneak use opens the drive control GUI; DriverFileSystem supports managed/raw mode and locks on both floppy and HDD. DriveData.setUnmanaged clears the lock and removes the old volume address only when the selected mode actually changes. The old content lives under that old address, so the new volume starts empty. Switching modes is intentionally destructive; the existing screen warning and ability to switch a locked disk's mode remain.

Ordinary floppy use now opens the existing DriveMenu in either hand. Sneak-use does not open it, and controls stop working when that stack is no longer held. Current factory-tagged and legacy lootPath/lootFactory disks cannot use the destructive mode menu. Mode/lock data helpers resolve legacy oc:floppy data through the floppy driver, instead of incorrectly using only HDD legacy data.

DiskItemEnvironment shares the existing environment construction/save forwarding across HDDs and ordinary floppies. Floppies use floppySize, one platter and their original access sound/speed. Managed mode exposes filesystem, raw mode exposes drive. Locks protect the managed filesystem/label and raw byte/sector/label callbacks; mode and lock owner survive save. Loot factories retain their existing registration/content path and use the same save-forwarding wrapper without changing factory ownership.

With the current NBT-backed native storage, a real mode change must remove the old node address, active fs/raw-byte payload, owner/handle mapping and head position, while retaining the label, unrelated tags and other node fields. This prevents old files/raw bytes reappearing when toggling back. Selecting the current mode preserves content, address and lock. ItemStack setters update a copied driver tag and replace CustomData: already-copied inventory snapshots remain unchanged, allowing normal inventory synchronization to observe the new state. Legacy archives outside the active driver tag are not deleted or migrated.

Raw readByte retains upstream's signed byte result: stored0xff reads as-1. The port does not reinterpret this as255. Existing binary filesystem streams are unaffected.

## Verification

- Initial disk-mode-red compilation failed because the fixture used the wrong mock-player helper signature; it is not a runtime baseline. disk-mode-corrected-red had4 real feature failures and one representation-only assertion that compared absent versus explicit default-false mode metadata. That control was normalized before the final baseline.
- disk-mode-final-red completes832 tests with6 required failures: missing floppy menu, raw mode absent, managed floppy writable while locked, reused volume identity, mutable inventory snapshot aliasing, and ignored legacy mode configuration. Logs have archived build/latest/debug variants. disk-mode-format-red has1/6 focused units failing selective storage/identity reset.
- First disk-mode-green had2 test-oracle failures expecting unsigned255 from readByte. Reference Drive.scala plus ResultWrapper confirm signed Byte(-1); only expectations were corrected, not production byte semantics.
- disk-mode-fixed passes build/all2206 units/all832 ordinary GameTests. Seven new server cases test held-hand/menu ownership/sneak, raw capacity/platter/label/lock/data, managed lock/binary content, mode cycles/new identity/empty contents/retained label, current-mode and loot protection, inventory snapshot isolation, and legacy configuration. Two new units protect selective reset versus same-mode preservation.
- disk-mode-final-mixed passes build/all2206 units/all922 tests with JEI/CC/AE2/Mekanism/EnderStorage/ProjectRed and external world-loot fixtures, complete summaries and normal exit.
- The native restart fixture now uses four physical disk drives (world loot, legacy loot, locked raw floppy, locked managed floppy) and an internal locked HDD. In oc-restart-world-loot-floppy-mode-20261020-01, prepare PID69044 and fresh-JVM verify PID58640 each pass838 required tests and exit normally. Four binary read handles resume at offset1, local731 and all filesystem/drive/host identities persist. Raw0xff/signed-1, raw/managed mode, labels and owners survive; raw byte/label writes and managed write-open are rejected after restart. External world source bytes remain unchanged. Logs disk-mode-restart-{prepare,verify}-{build,latest,debug}. This world is already verified; use a new world name for another prepare/verify pair.

The existing guarded restartWorldLoot/restartPhase/restartWorld profile and tracked seeder in [[World loot disks]] reproduce this expanded storage probe. Earlier checkpoints with fewer drives/handles remain historical evidence for their source versions.

## Remaining scope

This proves server-side use/menu behavior, actual native Lua5.3 hardware and process restart on Windows. It does not prove real-client rendering/input/packet delivery, survival/multiplayer/protection/platform/load coverage. No client, OS Computer Use, Modrinth installation or production operation occurred. Native managed HDD/floppy/RAID still use RAM/NBT instead of the upstream save-directory/bufferChanges backend, and native raw drives still store inline bytes instead of the old external compressed volume. That separate backend/migration work must preserve current contents, identities and handles. Mode/lock tooltip/style polish remains deferred. Full port goal is active; beta estimate3-6 concentrated workweeks, low confidence, not a feature percentage.

## Raw world storage follow-up — 6cf4c2890fe2b2869fa5d84d70abe9cdbb2b804a

Raw HDD/floppy now use independent oc:rawStorage metadata and external GZIP .bin files. Real mode changes clear that binding; same mode preserves it. Native factories need the active world in either mode. Three existing raw defects fixed: aliased standalone snapshots, stale tail after shorter reload, headPos always reset0. All2237 units/938 mixed tests per JVM pass, actual GZIP bytes and no-inline metadata checked across fresh process pair; [[Native volume import]] contains full evidence/limits. This does not establish client/Survival/multiplayer or powerloss acceptance.
