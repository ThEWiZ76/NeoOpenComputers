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


## 2026-10-20 real-client findings

User-assisted overlay search/right-click on RAID opens the plugin's Manual category on installed9bc57db with JEI19.27.0.343. MCP can click the category button, while inventory overlay search/click and Ctrl+F injection did not focus JEI; the mod-specific keybinding is unsupported by the MCP tool. Actual category rendering is confirmed for this mixed profile, not all callback pages/input paths.

The first link click renders the old home page; reopening the manual renders Raid. ManualRegistry.navigate updates history without refreshing an existing screen, so fix6f70021b880e369e30beabb9cab7d9108bf9ba37 selects the path BEFORE opening/initializing ManualScreen. Build/all2188 units pass. No additional whole-server rerun was needed for this client-only ordering correction; server874 mixed and both805 restart summaries belong to the preceding addon-factory runtime. A new profile jar/restart is still required for fixed-first-click acceptance.

Raid's legacy oredict:oc:raid image renders a red question mark. Investigate the OC content-alias vs modern-tag resolution in OreDictImageProvider rather than claiming manual visual parity. API tabs/multipage wrapping/order for a callback-bearing item, input bridge, reconnect, minimal profile and the rest of JEI scope remain open. See [[Minecraft test window]] for installed hash/PID/screenshots and user-confirmed screen3 placement.

Image follow-up653ce942d0c1022425776eb98ef928e1d2bda7a7 resolves legacy oc:* images through the existing content API after modern tag lookup. Dedicated-server stack selection passes, including OpenOS metadata; build/2188 units/802 absent/877 final mixed pass. Client first-click navigation and icon pixels still require the prepared newer jar/restart. See [[Manual content images]] for artifacts, remaining stale pages and the separate intermittent AE2 fixture failure/guard evidence.

## Updated-client acceptance and ordering gap

After the user stopped the old client, c78a0659749ee4e1039b5d844be7fdafb7654573 was installed and hash-verified. The user restarted and confirmed screen3; fresh PID64432/MCP/title screen and the installed hash confirm the new instance. Codex opened existing testworld through MCP only. Desktop Computer Use remains off. The mixed profile still has JEI19.27.0.343 and Sodium/Iris, whereas automated optional-provider gates pin JEI19.21.0.246.

User-assisted RAID overlay lookup again exposes RecipesGui's Manual category. ONE MCP click on Open Manual immediately renders the Raid title, correct text and the real RAID block image. Evidence: updated-jei-raid-uses.png and updated-jei-raid-first-click.png. This closes those two specific installed-client regressions; it does not approve every manual page or the whole JEI port.

User-assisted graphics-card overlay lookup exposes Crafting, API, Manual in that visible order. MCP selects API and advances through all14 GPU callback pages. All14 screenshots and updated-jei-graphics-api-contact.png retain the evidence; methods appear alphabetically with signatures/descriptions, working pagination and no observed panel overflow. This sample does not establish Unicode, every provider, every GUI scale or minimal-profile acceptance.

The category order is a confirmed remaining parity gap: upstream ModPluginOpenComputers.scala explicitly wants Manual before callback documentation, despite registering the categories in the same order as this port. The pinned JEI RecipeCategorySortingConfig defaults to natural recipe-type UID order after vanilla priorities; api sorts before manual. The actual test profile's recipe-category-sort-order.ini also lists neoopencomputers:api before neoopencomputers:manual. Registration order alone is therefore not evidence of display order. No user sort configuration was changed. Investigate the supported category-order contract and stable UID/migration consequences before fixing it.

Runtime disk metadata transport/addition/deduplication/detach, hidden ingredients, GUI exclusion geometry, icon animation, subtype identity/loot cycling/reload/reconnect/input bridge acceptance and other profiles remain open. Existing inventory overlay input limitation still requires user help for a different search; once RecipesGui is open, category navigation and page controls work through MCP.

## Category ordering and definition shortcuts

Runtime5fb2569360c70c2da3ca37830b6f3793b85007b1 changes the callback category UID from neoopencomputers:api to neoopencomputers:manual_callbacks. JEI has no public category-priority hook in the pinned plugin API; the new stable UID naturally follows neoopencomputers:manual. The plugin does not edit JEI config files. JEI itself drops the obsolete API UID and appends the new type when loading its sort file: a preference specifically tied to the old API UID resets, while all other category priorities remain. A later explicit callback-first preference for the NEW UID is respected. No general JEI configuration migration or internal sorter reflection is added to production.

One optional GameTest invokes the real pinned RecipeCategorySortingConfig against disposable files. The old UID fails default ordering; fixed build/2190 units/814 JEI-only tests pass. It covers fresh defaults/vanilla crafting priority, an existing old-api/manual list with unrelated custom priorities, and an explicit valid callback-first order whose file remains unchanged. The full892 mixed and810 absent gates for c1717cb49 also pass. Logs jei-category-order-{red,fixed}-{build,latest,debug}.log. This is server-side execution of the actual sorter, not rendered acceptance of the new UID in the still-old client.

A subsequent actual-client Manual click for the GPU reveals a second definition-path defect, despite the successful Raid sample: %LANGUAGE%/item/graphics_card_tier1.md is absent. Runtime c1717cb49 fixes shared alias-to-document resolution and upstream excluded shortcuts, with42 binding cases. See [[Manual document paths]] for the corrected baseline, full gates, staged jar/hash and remaining actual-client acceptance. Do not generalize the Raid screenshot to all item links.
