# EEPROM reprogramming and cold boot

The real Lua hot-swap test uncovered a stale boot-source concern. LuaArchitecture.recomputeMemory initially reads EEPROM code, but EepromEnvironment.set updates the item without recomputing memory. Subsequent stop/start reused the old bootSource. Additionally, LuaArchitecture.load could restore old architecture bootSource after newly programmed EEPROM hardware was loaded.

LuaArchitecture.initialize now reads the currently installed EEPROM from its bound machine host before compiling a new boot thread. Unbound/test architectures without installed EEPROM retain their explicitly supplied source. This is a cold-start source refresh, not preservation of a running Lua heap across reload.

Two real robot/EEPROM GameTests boot a first program, wait for its explicit phase color, call the actual EEPROM set callback with a second program, stop the computer, then either restart directly or save/load the stopped robot before starting. Both require the second program's different color. eeprom-reboot-red.log reproduced stale execution in both cases. No fixed wall-clock sleeps or random assertions.

Live GUI flashing, restart requests originating within Lua and full Lua coroutine persistence remain separate verification work. Other runtime components and full port matrix remain open.

Verification: eeprom-reboot-final.log passed all 497 required GameTests, including both new regressions, but three unit mock hosts returned null instead of an empty hardware iterable. Corrected those fixtures without changing assertions. eeprom-reboot-unit-verified.log then passed test/build: 2059 unit tests zero failures/errors. Production code and artifact unchanged between the successful GameTest run and final unit run. SHA256 0FED24F4EDED8A0975F5FC350934EDCBC7C855EB9065E495CB76AF00CB240B58. git diff --check clean. Not installed/live-tested; Minecraft client remains closed.

## Lua-initiated reboot

Added an end-to-end robot program that writes a persistent rebooting marker, rewrites its own EEPROM code through the real component callback, then calls computer.shutdown(true). The old program raises an error if it ever resumes after shutdown and rejects a second execution of the old source. The replacement program verifies the marker, writes rebooted, sets a distinct completion light and stays running. This complements externally triggered restart/save-load tests with the actual Lua shutdown path and EEPROM callback pauses.

Verification: eeprom-lua-reboot.log full test/build/GameTest success; 2059 unit tests zero failures/errors and all 499 required GameTests. No additional production fix was needed after the previous EEPROM refresh change. Artifact SHA256 F79C949DFCB2DC171EAFE8547D488ABEB25ACB5E9F289B554ECCA46BF1785A1C. git diff --check clean. Not installed/live-tested; client remains closed.
