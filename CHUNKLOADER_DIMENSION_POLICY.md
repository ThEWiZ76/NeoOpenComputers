# Chunkloader dimension policy

Related: [[../Project]], [[../02 plans/Complete portering]], [[../04 ai-context/Port gap triage]], [[Process restart test]], [[Network addresses]].

Runtime c27ef60642e42ff32e430d9c3a9973bcfcfec5da, 2026-10-20: build and all 2132 unit tests pass. Fresh world oc-restart-chunkloader-policy-20261020-01 passes all 725 required GameTests in prepare PID59012 and verify PID43124, with ProjectRed and ComputerCraft together and normal shutdowns. This includes all 721 ordinary tests and four opt-in restart probes. No client or Computer Use action occurred.

## Contract and compatibility

Upstream Settings.scala/application.conf and server/component/UpgradeChunkloader.scala at 7d9eac5584e238a890a388a0f697c27c7391caa4 define chunkloader.dimWhitelist and dimBlacklist. Empty whitelist allows any dimension not blacklisted; a nonempty whitelist limits access, and blacklist always wins. The port now retains these paths using namespaced dimension IDs instead of obsolete numeric IDs. Both defaults are empty. There are no wildcard or dimension-tag rules; unknown but syntactically valid namespaced IDs can be configured for mod dimensions.

Manual setActive(true) in a forbidden world raises exactly this dimension is blacklisted. Automatic computer.started messages do not raise an error and cannot activate there. Restoring active state in a forbidden world clears activation; a later policy relaxation does not silently resurrect that rejected request. On a policy update, existing live loaders release their tickets on the next component update without draining power. Temporary network detachment still retains the requested active state as documented in [[Network addresses]].

NeoForge restores mod tickets before normal component ticks. A TicketController loading-validation callback discards this controller's saved block/entity tickets when their world is forbidden, before they become active. Checking only the component update would leave persisted tickets behind. Live ticket administration now remembers the actual ServerLevel that owns its tickets, so moving the host to another dimension releases the old world's tickets before requesting any in the destination. The existing 3x3 ticking-chunk footprint is unchanged.

Saving always writes the current active boolean. Previously a reused component-data tag could retain active=true after disabling, causing a later restore to reactivate it. A focused unit test reproduced this and now protects the disabled state.

## How to run/use this

In the mod's existing neoopencomputers-common.toml configuration, use the chunkloader section:

```toml
[chunkloader]
dimWhitelist = ["minecraft:overworld", "example:moon"]
dimBlacklist = ["minecraft:the_nether"]
```

Set both lists to [] for the original unrestricted default. An ID on both lists is denied. Names must be valid namespaced IDs; numeric entries are invalid and NeoForge applies its normal config correction. Convert an old configuration explicitly: vanilla 0 becomes minecraft:overworld, -1 becomes minecraft:the_nether, and 1 becomes minecraft:the_end. For custom old dimension IDs, resolve their intended modern replacement rather than guessing. This does not import 1.12 worlds or automatically convert a legacy config file. No production configuration was changed for this checkpoint.

From M:/development/OpenComputers, run the isolated restart regression sequentially, using a fresh world name and waiting for each process to finish:

```powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithProjectRed -PwithComputerCraft '-Pneoopencomputers.restartPhase=prepare' '-Pneoopencomputers.restartWorld=oc-restart-policy-example-01' --no-daemon --console=plain
rtk proxy cmd /d /c gradlew.bat runGameTestServer -PwithProjectRed -PwithComputerCraft '-Pneoopencomputers.restartPhase=verify' '-Pneoopencomputers.restartWorld=oc-restart-policy-example-01' --no-daemon --console=plain
```

The opt-in test generator changes only this test process's cached Nether blacklist before worlds load in verify. Prepare saves nine actual controller tickets and a PID/phase record. Verify requires another PID and inspects real ticket data without instantiating/updating a replacement loader, so an update-time cleanup cannot mask missing startup validation. Test worlds/logs remain for inspection; the probe is absent in ordinary runs. The pair also retains existing native computer/tablet/microcontroller and CC redstone restart regressions.

## Evidence and limits

Three ordinary GameTests reproduce whitelist exclusion/blacklist precedence, blocked save-load/automatic activation and old-world ticket leakage on a dimension transition. They inspect actual ForcedChunksSavedData for all nine chunks, check activation results and no blocked power use, and restore cached settings in finally blocks. The transition fixture changes a generic host's real ServerLevel; it does not claim end-to-end robot/drone portal or multiplayer acceptance.

Retained evidence in build/finish-port-implementation/:

- chunkloader-dimension-policy-red.log: three ordinary required failures before policy/world tracking fixes.
- chunkloader-stale-active-red.xml: disabled save retained a previous true flag.
- chunkloader-policy-restart-red-prepare.log and chunkloader-policy-restart-red-verify.log: prepare PID21768 passes725; verify PID44064 fails only the persisted Nether ticket assertion even though policy was blocked before world load.
- chunkloader-policy-restart-prepare.log and chunkloader-policy-restart-verify.log: final pair PID59012/PID43124 both pass725 and shut down normally; verify explicitly records saved-ticket rejection.

Still open: real-client activation/error presentation, actual robot/drone portal flows, crash and long-load acceptance, broader custom-world/platform behavior and other chunkloader/addon semantics. The fixture tests policy rejection of persisted tickets; it does not prove removal of every historical orphan ticket in allowed worlds.
