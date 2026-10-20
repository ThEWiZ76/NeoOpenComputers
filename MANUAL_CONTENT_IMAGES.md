# Manual content images

Image runtime653ce942d0c1022425776eb98ef928e1d2bda7a7, AE2 fixture guard26aaf15f209a520b0aa269191b6e8036ca3bb7c4, 2026-10-20. Related: [[Minecraft test window]], [[JEI documentation integration]].

## Failure and correction

Actual-client acceptance on installed9bc57db showed a red missing-image question mark on the Raid page, whose markdown image is oredict:oc:raid. Upstream OreDictImageProvider queried OreDictionary entries, including OC's registered content names. The port queried only modern item tags. It therefore missed legacy OC aliases and camel-case names that are not even valid ResourceLocation paths.

OreDictImageProvider still prefers a populated modern item tag and cycles its complete ingredient list. If no tag resolves, an oc: prefix now resolves through the existing public item registry with the exact remaining alias. Its ItemInfo creates the full stack, preserving registered special metadata such as OpenOS factory identity. No ad-hoc duplicate item map or resource-ID lowercasing is added. Missing/invalid/uninitialized names retain the warning renderer.

## Evidence and limits

Three actual-registry GameTests check RAID, CPU, graphicsCard1 and materialALU aliases against the expected real items; OpenOS special stack component equality; and multi-item minecraft:logs plus missing-name warnings with API.items temporarily unavailable. The API is restored in finally. The renderer's stack selection is tested on the dedicated server without invoking GPU drawing.

The unchanged implementation first fails two of802 no-optional tests: RAID content alias and OpenOS special-stack image. An earlier test compile failure accessed a package-private renderer directly; it is fixture setup failure, not runtime reproduction. The fixed build/all2188 units and all802 no-optional tests pass. The first877-test mixed run passes the image cases but fails an existing AE2 interface-crafting wait for4000 real logs after CPU removal. Its logs/world are retained.

The AE2 fixture now reacquires the live drive's grid after CPU-removal topology change rather than retaining only its initial reference; quantity, recipe, CPU and conservation assertions are unchanged. All877 tests pass in the subsequent complete mixed run with CC/AE2/Mekanism/EnderStorage/ProjectRed/JEI, normal exit and full summary. The conditional rebinding log did NOT fire in that green run. Treat stale-grid retention as guarded, not as conclusively proven cause of the intermittent prior failure. Keep the AE2 wait failure as a diagnostic open point if it recurs; do not claim production AE2 behavior was fixed by this fixture change.

Evidence: build/finish-port-implementation/manual-images-red-server-build.log, manual-images-red-latest/debug.log, manual-images-mixed-build/latest/debug.log (AE2 failure), manual-images-absent-build/latest/debug.log, manual-images-mixed-rebound-build/latest/debug.log (green). Ordinary counts are802 without optional mods and877 mixed. No new process-restart claim.

A lexical scan finds98 distinct oredict:oc:* references across bundled translations. Five names lack observed registration: abstractBusCard, worldSensorCard, accessPoint, switch, serverRack. hoverBoots, initially absent from declared-constant scan, IS registered literally and must not be reported missing. The first two pages describe StargateTech2/Galacticraft integrations; accessPoint/switch are explicitly deprecated upstream pages; old German serverRack content needs a separate current-rack documentation audit. Do not fabricate mappings that imply unsupported/deprecated devices exist. The image-provider correction is not a claim that every old translated manual page is current or rendered correctly.

## Client acceptance pending

Current client PID68248 still runs9bc57db. Prepared tested jar build/finish-port-implementation/neoopencomputers-manual-images-653ce94.jar SHA256E4AFC00645491D057AA319C402EF5C5E0D8254078EE0DBFF6D3BE5D73E13BEEE includes the addon-factory identity, JEI navigate-before-open and image-provider corrections, plus the AE2 fixture guard. It has not been installed or restarted. Fixed first-click RAID page and actual rendered block icon require this new artifact in the named test profile, a coordinated full restart on screen3 and screenshots. MCP can operate ManualScreen and the JEI category button; inventory JEI search still requires the previously documented manual input assistance. Desktop Computer Use remains off.

Whole-port beta estimate stays3-6 concentrated workweeks, low confidence, including remaining implementation and client/survival/multiplayer/platform acceptance. Test totals are not a completion percentage.

## Installed-client acceptance and layout follow-up

On installed c78a06597/PID64432, a first click from the actual JEI RAID Manual category now opens Raid with a correctly rendered RAID block image. Evidence updated-jei-raid-first-click.png closes the earlier missing-image and stale-initial-page regressions for this sample. Screen3 is user-confirmed; all subsequent control is MCP, desktop Computer Use off. See [[JEI documentation integration]] and [[Minecraft test window]].

That screenshot exposed an independent word-wrapping defect: plain text beginning with a separator after an inline link can split a normal word (for example which) across rows. fittingText treated the initial space as the end of the first word, bypassing its whole-word fit check. Runtime 3751c7675ac8183bc92d8e2d858762ecb1aceb12 measures the leading separator plus the first actual word for remaining-line fit, and permits normal leading-space removal when measuring next-line fit; code indentation remains preserved. The new regression covers both normal-width words and a word exactly as wide as the full line. Baseline ManualScreenShapeTest has one failure among46 tests; final build/all2190 units pass. No server rerun for this client-only layout change. Logs manual-word-wrap-{red,fixed}-build.log and retained manual-word-wrap-red.xml.

The new layout fix is NOT in the running c78a06597 client. Prepared build/finish-port-implementation/neoopencomputers-manual-word-wrap.jar SHA256 6653CE4E236A47847509D3166AD904799609EC8B8B89AABFCC21A0A15DA914E6; batch installation with a later agreed client update and then verify rendered word boundaries. Avoid another restart solely to check this small fix. The previous staged creative-template jar remains the exact currently installed artifact. API/category order is separately open; full translated-manual validation is not claimed.

## GPU client acceptance and punctuation wrapping

Actual installed177357b client, user-confirmed screen3, Minecraft MCP only: JEI GPU tabs now display Crafting/Manual/API. The first Open Manual click opens Graphics Card with the native GPU item image and bundled content; the earlier missing graphics_card_tier1.md page is resolved. Screenshots metadata-client-gpu-{categories,manual-category,first-click}.png are retained under build/finish-port-implementation. This is GPU acceptance at the current GUI scale, not all item/locale/scale acceptance.

The same first-click screenshot exposed text joining as screen.Graphics. A measured-width layout regression reproduces the loss: fittingText chooses punctuation as the last fitting chunk, appendTextEntries removes the following separator and then reuses the same row for the next word. Runtime71ab1058b5ae1fb1648754d91152be9f516ddb2e always advances after a partial fitting chunk; complete inline segments still share a row. Baseline ManualScreenShapeTest fails1/47; final build/all2191 units pass, zero failures/errors/skips and normal exit. Logs manual-separator-{red,build}.log. This fix is pushed but not installed in the currently running177357b client; rendered separation still needs the next bundled client update. No server rerun for this client-only layout change.
