# Native Lua persistence probes

See [the feasibility report and run instructions](../../NATIVE_LUA_PERSISTENCE.md).

- `verify.ps1` runs a pinned standalone JNLua/Eris save/restore proof in separate Java processes on Windows x64.
- `NativeLuaPersistenceProbe.java` validates local closures, nested coroutines, cyclic tables and rebound Java callbacks.
- `RunningRackPersistenceProbe.java` is an opt-in **currently failing** Minecraft parity gate. It is intentionally not part of the ordinary passing suite; the mod still loses Lua continuation on reload. Promote it to a required GameTest when integrating native persistence.

These tools do not install a Minecraft runtime, alter Modrinth or claim full persistence support.
