# EEPROM reprogramming and cold boot

The real Lua hot-swap test uncovered a stale boot-source concern. LuaArchitecture.recomputeMemory initially reads EEPROM code, but EepromEnvironment.set updates the item without recomputing memory. Subsequent stop/start reused the old bootSource. Additionally, LuaArchitecture.load could restore old architecture bootSource after newly programmed EEPROM hardware was loaded.

LuaArchitecture.initialize now reads the currently installed EEPROM from its bound machine host before compiling a new boot thread. Unbound/test architectures without installed EEPROM retain their explicitly supplied source. This is a cold-start source refresh, not preservation of a running Lua heap across reload.

Two real robot/EEPROM GameTests boot a first program, wait for its explicit phase color, call the actual EEPROM set callback with a second program, stop the computer, then either restart directly or save/load the stopped robot before starting. Both require the second program's different color. eeprom-reboot-red.log reproduced stale execution in both cases. No fixed wall-clock sleeps or random assertions.

Live GUI flashing, restart requests originating within Lua and full Lua coroutine persistence remain separate verification work. Other runtime components and full port matrix remain open.

Verification: eeprom-reboot-final.log passed all 497 required GameTests, including both new regressions, but three unit mock hosts returned null instead of an empty hardware iterable. Corrected those fixtures without changing assertions. eeprom-reboot-unit-verified.log then passed test/build: 2059 unit tests zero failures/errors. Production code and artifact unchanged between the successful GameTest run and final unit run. SHA256 0FED24F4EDED8A0975F5FC350934EDCBC7C855EB9065E495CB76AF00CB240B58. git diff --check clean. Not installed/live-tested; Minecraft client remains closed.
