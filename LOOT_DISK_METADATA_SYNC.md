# Loot disk metadata synchronization

Runtime 177357bbe65ef1b60ccf0ac09a063fba503e8c7b, on feature/finish-port. See [[Loot disk cycling]], [[JEI loot disk cycling]] and [[Minecraft test window]].

## Contract

All registered floppy templates, including disks excluded from recrafting, are retained privately by factory ID. Cycling eligibility has a separate ordered ID set: enabling a previously excluded disk appends it, updating an enabled disk preserves its position, and disabling/re-enabling appends it again. Registration results, snapshots and payload accessors return copies.

LootDiskNetworking queues PlayerLoggedInEvent and sends a reset followed by one copied stack per registered disk at the next server post-tick. Disconnected/replaced players are skipped; server shutdown clears pending entries. The required play-to-client channel is neoopencomputers:loot_disk_metadata, version1. Its ItemStack.OPTIONAL_STREAM_CODEC uses the empty stack as reset. Client/server must use matched experimental builds. No filesystem callable or filesystem contents are serialized or evaluated.

LootDiskClientState is separate from API.items, including integrated-server state. It validates native floppy/factory data, deduplicates full item components and normalizes count. Login/logout reset it. Client post-tick flush batches notifications; late subscribers receive retained metadata on the next flush, and closed subscriptions cannot receive stale callbacks. This is a login snapshot, not automatic broadcasting of registrations made later; explicit snapshot resend remains possible.

Creative contents merge local templates with server metadata by factory ID, including opt-outs and server-only disks. Server display metadata wins on collisions without modifying local factory registration or cycling eligibility. On a live connection, client refresh rebuilds the OC main/search tabs through public buildContents and updates connection search trees.

The JEI runtime subscribes when available, queries existing ingredient components and adds only missing copied stacks on the client tick. Runtime teardown closes the subscription; replacement gets a fresh subscription. The real pinned JEI IngredientSet replaces values sharing a floppy factory UID. No runtime manual/callback recipes are registered here; their lookup behavior for remote-only disks remains part of client acceptance.

## Verification and limits

Final build and all2190 unit tests pass. All899 mixed-provider GameTests and all816 absent-provider GameTests pass, with complete required-test summaries and normal terminal exits. Evidence: loot-metadata-final-{mixed,absent}-{build,latest,debug}.log under build/finish-port-implementation.

Coverage includes codec ownership and flags, reset/dedup/batching, late and detached subscribers, real creative output merging, eligibility order and a real pinned JEI IngredientSet using native floppy subtypes. The order regression fails before its fix. Dedicated tests deliberately avoid an IIngredientManager proxy because client Rect2i is stripped there; the existing JUnit runtime stub supports the manager and checks that teardown prevents subsequent queries. Earlier loot-metadata-mixed logs have passing GameTests but failing unit stubs; they are not a passing full gate.

Actual TCP login timing, real IIngredientManager notifications, creative/search rendering, remote disk recipe/manual lookup and disconnect/reconnect cleanup are still open. Headless tests do not prove these client behaviors. External/world loot source loading also remains open. Full port beta estimate remains3-6 concentrated workweeks with low confidence.

## Installed client artifact

The Modrinth NeoOpenComputers test instance now contains the tested combined jar with SHA256 BFB6630D433CB4CD74BA82E2B4270A191C614C6B9CB989DCE7F0EE8C98085D1E. It includes prior manual path/category/word-wrap fixes and authoritative recipe snapshots. Installation copied only neoopencomputers-0.1.0.jar after checking the target profile was stopped; unrelated Java/Minecraft processes were left running. Previous c78a065 artifact is preserved as build/finish-port-implementation/client-backup-c78a065.jar. User confirmed the restarted client ready on screen3; MCP opened testworld successfully. Visual and TCP acceptance is in progress; no new-client visual acceptance is claimed. Computer Use remains off; further interaction uses the Minecraft MCP.

## Real TCP evidence and remaining functional defects

Installed177357b client connected to an isolated NeoForge21.1.234 dedicated server on127.0.0.1:25566, JDK21.0.10. Server-only oc_loot_sync_fixture registers one eligible orange and one excluded green disk; the client has no fixture addon. Initial handshake required matching existing Mekanism and fps-display network channels; the isolated server also includes matching Mekanism addons, Powah, GuideME and Cloth Config from the existing test profile. Client files are unchanged. Minecraft/RCON listeners independently verified on127.0.0.1 only; fixture and server files are ignored acceptance artifacts. No OS Computer Use.

Fresh client log records real JEI IngredientManager additions of2 disks after runtime startup. Native creative search proves both exact factory IDs, labels, colors and recipeCycling true/false metadata. Explicit fixture late registration/resend produces a chat confirmation and an actual JEI runtime addition of1 disk. Logs metadata-tcp-client-before-restart.log and metadata-tcp-server-before-restart.log, screenshots/data metadata-tcp-search-two and metadata-tcp-three-confirmed under build/finish-port-implementation.

The native creative name search nevertheless retains only2 disks after late registration, reopening and clearing/retyping the query. The previous connection.updateSearchTrees() only invokes rebuildAfterLanguageChange, replaying previously captured lists. Runtime8344ac4bdd18ddb808afb19bcce1aeb724502895 replaces this with public SessionSearchTrees.updateCreativeTooltips/updateCreativeTags using a fresh current search-tab list. Build/all2191 units pass with normal exit; actual updated-client proof is pending. This client-only change is pushed but not installed; do not call the real search defect fixed yet.

User independently confirms right-clicking the excluded server-only disk in JEI does nothing. Static recipe/manual ingredient indexes do not incorporate ingredients added after registration. A functional fix is in progress: dynamic lookup of actual received crafting recipe holders, plus runtime documentation registration once per factory. It is not yet a verified checkpoint. Disconnect/reconnect cleanup, real crafting/result/wrench behavior and updated-client search/lookup remain open. The local server was stopped normally after reproducing these defects; client is left at its disconnect screen.

## Runtime lookup implementation a656327a3

The functional server-only lookup fix is implemented and pushed. LootDiskRecipeLookup uses JEI's public typed recipe-manager plugin to find actual current RecipeManager crafting holders. Input lookup covers valid loot disks including opt-outs and compatible wrenches; output lookup uses the native factory subtype and actual assembled result. It never invents a crafting recipe or exposes a removed/disabled one. JEI's PluginManager combines internal/dynamic results with distinct(), so the same holder remains one recipe.

After adding missing runtime ingredients, OpenComputersJeiPlugin adds their manual/callback pages through public IRecipeManager.addRecipes. A per-runtime set seeded from initial documentation registration prevents repeated metadata or label changes from duplicating a factory's pages. Runtime replacement gets its own set and subscription; teardown behavior is preserved. Documentation pages own copied stacks and share the existing manual/callback generation logic.

Final build/all2191 units/all901 JEI+CC+AE2+Mekanism+EnderStorage+ProjectRed GameTests/all816 absent GameTests pass, complete summaries and normal exits. Two new optional tests use real pinned typed ingredients, the real server RecipeManager/holder and actual manual provider. They check late opt-out input, wrench, renamed output subtype, excluded output, ordinary-item rejection, recipe removal and copied runtime manual pages. Logs loot-runtime-lookup-jei-mixed-{build,latest,debug}.log and loot-runtime-lookup-absent-{build,latest,debug}.log. An earlier successful mixed-provider run used the wrong-case withJei property and did NOT load JEI; its mixed filename is not JEI proof. The final run explicitly uses withJEI.

Staged combined jar neoopencomputers-runtime-lookup.jar SHA2569D947DBEE2E7D51FE91949FEEE89C58A63D4D4621A4BD6C586336A48E1A0C7F7 includes8344ac4 creative search and71ab105 separator fixes. NOT installed; actual client remains177357b, local acceptance server stopped normally. No further cosmetic testing/restart is requested now. Actual updated-client search/right-click/crafting/reconnect gates stay open for a later functional acceptance batch. Next technical slice: reproduce upstream world-relative and buffered fromSaveDirectory behavior, which current FileSystemRegistry replaces with DirectoryFileSystem(Path.of(root),capacity) regardless of buffered. Upstream Settings.savePath is opencomputers/; existing port absolute-temp tests currently encode immediate writes even with buffered=true, so they do not prove that API contract.

## Wereld-lootdefinities en selectie —b4d462200

[[World loot disks]] vervangt de eerdere ontbrekende-wereldloaderstatus: wereldgebonden readonly factories, cycling/loginmetadata, gewogen selectie zonder gewichtexpansie en legacy wereldpad-fallback zijn geimplementeerd en gepusht. Build2203 units/908 gemengde/823 absent GameTests slagen; echte recipe-update-codec behoudt wereldoutputs na unload. Dit is nog GEEN chest-generatie: moderne loot-tablekoppeling en misc.lootProbability als gewicht zijn de volgende functionele stap. De upstream chesthook staat uitcommentarieerd ondanks de README/config; houd die functionele eis open. Geen client-/CU-actie; volledige portering en genoemde acceptatiegates blijven open.
