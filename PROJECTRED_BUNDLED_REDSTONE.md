# ProjectRed bundled redstone

The optional ProjectRed Transmission bridge uses the published 1.21.1-4.23.0 API. Its registration is isolated from ordinary class loading so installations without ProjectRed do not need its API classes. ProjectRed is not bundled or required by NeoOpenComputers.

## Behavior

- Input queries use the OC receiver position and physical side. The adjacent chunk must already be loaded.
- Six sides carry sixteen channels with unsigned strengths 0-255. ProjectRed bytes are decoded unsigned; ComputerCraft bits retain their established strength 15.
- When both transports provide input, each channel takes the larger strength, matching upstream integration/util/BundledRedstone.scala.
- An external IBundledTileInteraction exposes physical OC outputs to real ProjectRed cables without optional interfaces on core block entities.
- The advanced card callbacks and Creative visibility activate when either supported bridge registers. Basic cards and installations without a provider retain vanilla behavior.

Upstream references: integration/projectred/ModProjectRed.scala and common/tileentity/traits/BundledRedstoneAware.scala in the pinned local master-MC1.12 checkout. Modern API/setup: https://github.com/MrTJP/ProjectRed/tree/1.21.1/api and https://github.com/MrTJP/ProjectRed/tree/1.21.1 .

## Run the verification

Run these separately, waiting for each Gradle process to finish:

~~~powershell
rtk proxy cmd /d /c gradlew.bat build runGameTestServer -PwithProjectRed --no-daemon --console=plain
rtk proxy cmd /d /c gradlew.bat runGameTestServer -PwithProjectRed -PwithComputerCraft --no-daemon --console=plain
rtk proxy cmd /d /c gradlew.bat build runGameTestServer --no-daemon --console=plain
~~~

The optional development runtime pins ProjectRed Core/Transmission 4.23.0, CodeChickenLib 4.6.1.529 and CBMultipart 3.5.0.161. Ordinary builds compile against the API only. Optional runtime configurations must not publish these mods as Maven dependencies or include their classes in the release jar.

Five conditional GameTests check advanced callbacks without CC and four actual framed neutral cable fixtures. Each OC host type appears as both emitter and receiver across the four fixtures. All sixteen channels carry distinct strengths from 255 down to 15 and must return to zero after output removal. Test-only reflection creates a registered real ProjectRed multipart; production uses public API only. These are programmatic host/world tests, not a live player crafting or visual wiring check.

## Remaining acceptance

Runtime connection eligibility now follows the host output-enabled policy; see REDSTONE_OUTPUT_LIFECYCLE.md. Case/microcontroller endpoints in cable fixtures have real installed hardware and running native machines, and their stop action must clear the signal delivered by the actual cable. Real client cable appearance/player flow, wire chunk boundaries/restarts and multiplayer still require separate acceptance. Wireless redstone and other providers remain separate work. The basic ProjectRed screwdriver is covered by the later section below; Expansion's electric screwdriver and off-hand behavior are not covered by that acceptance. No Modrinth profile was changed or client launched for this checkpoint.

## Verified 2026-10-19

- Build and 2112 unit tests passed.
- All 667 registered GameTests passed with ProjectRed only, with ProjectRed plus ComputerCraft, and with neither mod installed.
- The five ProjectRed bodies run only with ProjectRed; the eight existing CC bodies run only with CC. Thus the combined run executes both integration groups; the ordinary run proves absence/class-loading/fallback, not cable behavior.
- Logs in build/finish-port-implementation/: projectred-bundled-verified.log, projectred-computercraft-verified.log and projectred-absent-verified.log. Initial missing callbacks/output are retained in projectred-bundled-red.log.
- An initial unit failure exposed eager verification of IBundledTileInteraction on the optional bridge. Moving API registration and calls behind the Loaded class fixed absence loading. The prior CC-only callback/Creative expectations were updated to recognize either installed transport.
- Archive inspection found zero mrtjp/ or codechicken/ classes and zero corresponding embedded jars; the generated Maven POM has zero mrtjp/io.codechicken/cc.tweaked dependencies.

## ProjectRed screwdriver

ProjectRedScrewdriver registers the existing OC wrench check and usage callbacks when ProjectRed Core loads, independently of Transmission. It recognizes the IScrewdriver API. Usage checks its main-hand canUse contract and calls damageScrewdriver only when the caller requests an actual use; eligibility checks do not consume durability. Optional API types remain in a lazily loaded class, and metadata orders OC after Core when present.

ProjectRedWrenchGameTests uses the actual registered screwdriver on an adapter with a Survival test player. A normal click closes the clicked side; a sneaking click closes the opposite side. Each consumes exactly one durability and the preceding eligibility check consumes none. The test sets unbreakableScrewdriver to false temporarily and restores its previous value in finally, so it does not assert against a varying configuration or save a config change. Test-only reflection accesses that config, while production uses public screwdriver API only.

The development option below loads Core and its dependencies without Transmission. The wrench test requires Core to have loaded; bundled tests skip there, while the existing advanced-card test still requires vanilla fallback without a bundled transport.

~~~powershell
rtk proxy cmd /d /c gradlew.bat runGameTestServer -PwithProjectRedCore --no-daemon --console=plain
~~~

Upstream reference: integration/projectred/EventHandlerProjectRed.scala. Modern interface: https://github.com/MrTJP/ProjectRed/blob/1.21.1/api/src/main/java/mrtjp/projectred/api/IScrewdriver.java . This acceptance covers the standard main-hand tool; charged/empty electric screwdriver, off-hand behavior and real-client interaction remain separate.

Screwdriver verification on 2026-10-19: build and 2112 unit tests passed; all 668 registered GameTests passed with Core+Transmission, Core only, and no ProjectRed/CC. Logs in build/finish-port-implementation/: projectred-screwdriver-verified.log, projectred-core-only-verified.log and projectred-screwdriver-absent-verified.log. projectred-screwdriver-red.log retains the initially missing registration. With Core only, the five bundled PR bodies and eight CC bodies skip while the real screwdriver body executes. Without either mod all six PR bodies and eight CC bodies skip; that run verifies absence/fallback, not screwdriver behavior. Earlier combined CC+PR cable evidence remains the separate 667-test checkpoint above.
