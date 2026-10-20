# JEI documentation integration

Runtime 19b84ae4d5e828b005a02d636c09884b580a9467; verified2026-10-20. This is the documentation/input stage of the full JEI port, not complete JEI or visual acceptance. Scope: [[../02 plans/JEI integration]]. Upstream: integration/jei/CallbackDocHandler.scala, ManualUsageHandler.scala and the input/runtime portions of ModPluginOpenComputers.scala.

The optional @JeiPlugin registers manual and callback-documentation categories, in that order. It collects every JEI ItemStack with an API Manual.pathFor result and resolves callback documentation via Driver.environmentsFor. Callback discovery walks class hierarchies without constructing environments on the client, honors callback aliases, validates the same public Object[]/Context/Arguments shape as the runtime and gives child declarations priority. Names are sorted and duplicate providers do not duplicate methods. Both legacy documentation separators are parsed; unstructured docs and unnamed signatures remain readable.

Pages preserve long entries in chunks of at most12 lines. Signature/description text is wrapped with the actual client font; descriptions allow for their indentation and both margins within160px. The manual-link input handler obeys JEI's simulate-versus-execute contract, only accepts the left mouse button, then uses the existing Manual.openFor/navigate APIs. Runtime availability connects the existing ItemSearch hover/focused-input hooks; replacement/unavailability detaches both subscriptions. JEI search focus can therefore suppress terminal shortcut forwarding through the existing shared policy.

Pinned API/test/runtime target: mezz.jei:jei-1.21.1-neoforge[-api]:19.21.0.246. API is compileOnly, the optional runtime is enabled by -PwithJEI and neither JEI classes nor a hard JEI dependency enter the built mod/POM. The publisher's setup is [documented here](https://github.com/mezz/JustEnoughItems/wiki/Getting-Started-%5BMinecraft-1.21-and-1.21.1%5D). Exact API sources, POMs and inspected loader metadata are retained under build/finish-port-implementation/jei-api/.

The initially investigated19.64.0.455 runtime requires NeoForge21.1.238, whereas this project/profile uses21.1.234. Its first attempt also exposed a filtered transitive config repository; after resolving that, the loader rejected the runtime. That rejected launch misleadingly ended with Gradle BUILD SUCCESSFUL/exit0 and ran no GameTests. It is not counted as passing evidence. Publisher metadata for19.57.0.451 had the same loader floor;19.21.0.246 permits the pinned loader. API compilation and the actual server launch confirm this selected target. No loader/profile upgrade was made.

Evidence: eight new unit cases cover inherited/aliased/invalid callbacks, duplicate/null providers, long/empty pages and width budgets, input simulation and runtime detach/replacement. The missing documentation implementation first failed compilation in jei-docs-red-build.log; the implemented model and plugin subsequently compiled. Final build/all2187 units pass. All868 combined server tests pass with JEI19.21.0.246 plus the five existing provider families. All793 no-optional tests pass. Both server processes exit normally and include their complete GameTest summaries. Logs: jei-documentation-compatible-*/jei-documentation-absent-* under build/finish-port-implementation; the rejected newer-loader and initial repository attempts are preserved separately. A server run with JEI does not initialize or visually validate its client plugin.

## Run and acceptance

Headless compatibility check from the repository root:

```powershell
rtk proxy cmd /d /c "gradlew.bat runGameTestServer -PwithJEI -Pneoopencomputers.gameTestWorld=oc-tests-jei-documentation-manual --no-daemon --console=plain"
exit $LASTEXITCODE
```

Require a GameTest completion summary, not just BUILD SUCCESSFUL. No Minecraft client was restarted or updated for this checkpoint. The named Modrinth client still runs the preceding RAID build and automatically pauses while unfocused. Coordinate a full jar/dependency/hash-verified restart on screen3 and resolve pause-on-focus-loss before client testing. Check JEI usage on an OC item, category order, inherited callback text/multipage Unicode wrapping, manual navigation and return, search focus versus terminal typing, hovered rack-module lookup, disconnect/reconnect and minimal/mixed profiles.

Remaining: loot-disk recipe displays/subtypes, runtime registered-disk addition, hidden ingredients, actual visible GUI exclusion areas and upstream icon-animation parity; real client/GPU/survival/multiplayer/load and full integration acceptance remain open. Beta3-6 concentrated workweeks, low confidence.
