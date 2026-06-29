# Alpha Smoke Matrix

This matrix maps first-alpha smoke claims to automated evidence and the remaining manual alpha smoke pass.

Screen world rendering stays frozen unless there is a focused repro, focused failing test, narrow patch, and screenshot evidence.

| Area | Automated evidence | Manual alpha smoke |
| --- | --- | --- |
| OpenOS boot | GameTests cover booting a computer case with OpenOS media and visible terminal output. | Start a tier 1 computer from valid boot media and confirm `/home #` prompt appears. |
| Computer case storage persistence | GameTests cover computer-case hard-drive state through NBT save/load. | Write a small file to a hard drive, save/reload world, and read it back. |
| Disk-drive floppy persistence | GameTests cover disk-drive and rack disk-drive floppy state through NBT save/load. | Insert OpenOS floppy in disk drive, save/reload world, and confirm media stays loaded. |
| Redstone | Unit and GameTests cover redstone card, computer-case redstone IO, screen redstone, charger signal, print signal, and redstone IO block behavior. | Toggle neighboring redstone and verify signal/component behavior in-game. |
| Modem | Network card, linked card, and modem-message tests cover broadcast, send, distance, wake-message, and signal payload paths. | Send a modem message between two computers in a local world. |
| Inventory | Inventory controller, driver registry, adapter/provider, and transposer coverage exercise item inventory access paths. | Use an inventory-facing component against a chest or compatible inventory. |
| Tank | Tank controller and upgrade tests cover tank API shape and no-tank behavior. | Use a tank-facing component against a compatible tank/fluid handler. |
| Transposer | Transposer registration, renderer, and component tests cover block/entity/component shape. | Move an item or fluid between adjacent inventories/tanks. |
| Printer and print | Printer, print item, placed print, redstone activation, texture fallback, tooltip, and render-model tests cover current print smoke paths. | Create a print item, place it, rotate it, and confirm the configured shape remains. |
| Manual and packaging | Manual link/resource tests, metadata tests, jar packaging tests, API jar tests, contributor docs, changelog, and alpha guide tests cover public artifact readiness. | Install `build/libs/neoopencomputers-0.1.0.jar` in a clean NeoForge 1.21.1 client and open the manual/creative tab. |

Manual alpha smoke should save crash reports, client logs, screenshots, and exact reproduction steps for any mismatch.
