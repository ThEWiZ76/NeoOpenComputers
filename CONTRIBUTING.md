# Contributing

NeoOpenComputers is a Java-first NeoForge 1.21.1 community port of OpenComputers.

## Direction

- Keep the port Java-first.
- Do not port Scala source into this repository.
- Use the original `master-MC1.12` OpenComputers branch as behavioral reference, not as a source tree to compile directly.
- Keep Lua/OpenOS assets and behavior as close to upstream as practical.
- Keep public API compatibility where it helps existing addons, but prefer maintainable NeoForge 1.21.1 code over preserving old implementation structure.

## License

Contributions are accepted under the project MIT license. Only contribute code, assets, documentation, or tests you have the right to submit under MIT-compatible terms.

When porting behavior from upstream OpenComputers or bundled libraries, keep existing notices and third-party license files intact.

## Development Rules

- Work from `develop` unless maintainers ask for another branch.
- Add or update tests for behavior changes.
- Run the focused test for your change before the broad gates.
- Before sharing a build, run:

```powershell
.\gradlew.bat test build --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
```

- Install `build/libs/neoopencomputers-0.1.0.jar` for testing, not `neoopencomputers-0.1.0-thin.jar`.
- Fully restart Minecraft after replacing the jar.

## GitHub Actions

Do not add `.github/workflows` yet. GitHub Actions stay disabled until the port is stable enough for CI without creating build-failure noise.

## Screen Work

Screen world rendering is visually sensitive.

Do not change screen renderer, model, glyph, or multiblock code unless all of these are true:

- One reproducible root cause is identified.
- One focused failing test or live repro proves the issue.
- One narrow patch addresses that cause.
- Screenshot evidence proves the result before handing a jar to testers.

Avoid alternating static model, renderer, glyph, and layout changes in the same slice.
