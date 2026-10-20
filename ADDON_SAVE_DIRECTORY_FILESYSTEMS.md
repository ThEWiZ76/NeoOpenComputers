# Addon save-directory filesystems

Functional-first port work,2026-10-20. See [[../02 plans/Complete portering]], [[Loot disk metadata synchronization]] and [[Addon floppy factory persistence]].

## World-local roots —126020134

Upstream server/fs/FileSystem.scala resolves fromSaveDirectory(root,capacity,buffered) beneath the current save root and Settings.savePath=opencomputers/. The port instead opened Path.of(root) relative to the process and accepted absolute host paths. Two real GameTests fail on baseline for those exact behaviors, then pass after the fix.

FileSystemRegistry now resolves the active ServerLifecycleHooks server dynamically per API call and captures that world's opencomputers/root path in the returned filesystem. A supplier constructor supports isolated tests; the default API does not capture a world at initialization. Without an active server it returns null. Absolute/drive-rooted/parent-traversing names are rejected. Empty/root-relative names and nested addon names remain valid. An unopenable directory returns null and logs a warning; an existing non-directory file is preserved. Existing process-relative port files are NOT migrated, moved or deleted automatically. Callers relying on the previous accidental absolute-path behavior must use a world-relative filesystem name.

Build/all2193 units/all818 absent-provider GameTests pass with complete required-test summary and normal exit. Tests include a real dedicated test-server save root, exact bytes, absolute-path rejection, switching two isolated world roots with the same addon/drive name, no-world behavior, traversal rejection and preservation of an occupied file. Existing temporary-directory tests now inject a world root and request buffered=false for their immediate-write assertions; their old buffered=true checks encoded the defect. Logs save-root-{red,fixed}-{build,latest,debug}.log under build/finish-port-implementation. Baseline has exactly2 required failures.

## Immediate filesystem handle restoration —cb34eac02

DirectoryFileSystem.save/load previously did nothing. A regression saving open binary input/output handles fails1/38 focused tests because the restored input handle is absent. save now records input/output handle IDs, paths relative to the filesystem root and exact positions, plus capacity.used. It does not close live handles. load closes previous streams and restores valid existing files with preserved IDs/positions and non-truncating rw access for saved outputs. New IDs avoid restored handles. Missing files, invalid paths/IDs/positions and duplicate IDs are skipped; restoring a missing output never creates it.

Final build/all2195 unit tests/all903 mixed JEI+CC+AE2+Mekanism+EnderStorage+ProjectRed GameTests pass with complete summary and normal exit. The binary test resumes reads and overwrites at the saved offset without losing the remaining bytes; malformed/missing handle tests preserve valid streams and create no ghost file. Logs save-handles-{red,fixed}-build.log and save-handles-mixed-{build,latest,debug}.log. The818 absent run belongs to the preceding root checkpoint, not a rerun after the handle change. No client test, production action or Computer Use was performed. Both runtime commits were pushed to feature/finish-port.

## Remaining work

buffered=true STILL selects the immediate DirectoryFileSystem and is not implemented. Next reproduce no-write-before-save, committed versus uncommitted close/reload, binary handles across save/reload, and disk-backed buffered state in another JVM. Reuse the existing memory filesystem and its handle serialization where possible; keep disk contents out of host NBT for buffered filesystems, preserve world-relative naming, avoid automatic migration/deletion of unrelated files and wait for pending disk saves before reload/close. Upstream buffers file bytes in memory and commits through a serialized IO worker at save time; file contents are loaded from disk, handles from NBT.

Source comparison also shows DirectoryFileSystem.spaceUsed currently counts bytes only, whereas upstream Capacity and the port MemoryFileSystem include file/directory entry costs. Existing saved-directory capacity tests use too-small128-byte capacities and do not prove that contract. Cover this alongside buffered capacity/error behavior rather than claiming complete addon filesystem parity now.

Real process-restart/native-addon-handle acceptance, external/world loot descriptor loading and client reconnect/lookup acceptance remain open. Full beta estimate remains3-6 concentrated workweeks, low confidence; these fixes close concrete storage defects, not the whole persistence phase.
