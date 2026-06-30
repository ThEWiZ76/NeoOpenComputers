# OpenComputers Bedienungsanleitung

NeoOpenComputers alpha note: robots, drones, and block microcontrollers are not available in this build yet; computer cases, servers, racks, screens, keyboards, storage, cards, and upgrades are the current alpha focus. Microcontroller case items are present for recipe/API compatibility only and are not alpha smoke targets.

OpenComputer ist eine Modifikation, welche dauerhafte, modulare, und hochkonfigurierbare [Computer](general/computer.md), [Server](item/server1.md), Roboter, und Drohnen zum Spiel hinzufügt. Alle Geräte können mittels Lua 5.2 programmiert werden, was unterschiedlich komplexe Systeme entsprechend der Anwendung ermöglicht.

Um zu lernen, wie man die Bedienungsanleitung verwendet, siehe [die Seite über das Handbuch](item/manual.md) (der grüne Text ist ein Link - du kannst ihn anklicken!).

## Inhaltsverzeichnis

### Geräte
- [Computer](general/computer.md)
- [Server](item/server1.md)
- Mikrocontroller (alpha unavailable)
- Roboter (alpha unavailable)
- Drohnen (alpha unavailable)

### Software und Programmierung
- [OpenOS](general/openos.md)
- [Lua](general/lua.md)

### Blöcke und Items
- [Items](item/index.md)
- [Blöcke](block/index.md)

### Guides
- [Erste Schritte](general/quickstart.md)

## Überblick

Wie oben erwähnt sind Computer in OpenComputers dauerhaft, was bedeutet, dass ein laufender [Computer](general/computer.md) seinen Zustand beibehält, wenn der Chunk in dem er sich befindet entladen wird. Das bedeutet, dass wenn ein Spieler sich vom [Computer](general/computer.md) entfernt oder sich ausloggt, behält der [Computer](general/computer.md) seinen Zustand und fährt von diesem Punkt wieder fort, wenn der Computer sich dem [Computer](general/computer.md) nähert. Außer bei [Tablets](item/tablet.md) funktioniert dies bei allen Geräten.

Alle Geräte sind modular und können mit einer großen Palette von Komponenten zusammengestellt werden, ähnlich wie es bei Computern im echten Leben der Fall ist. Spieler, die gerne basteln werden in der Lage sein, die Geräte nach ihren Wünschen zu optimieren. Auf Wunsch können sie auch wieder [auseinandergebaut](block/disassembler.md) und neu aufgebaut werden, wenn die erste Konfiguration nicht befriedigend war. Bei [Computern](general/computer.md) und [Servern](item/server1.md) können die Komponenten sofort ausgetauscht werden, indem einfach die entsprechende GUI geöffnet wird.

OpenComputers-Geräte sind kompatibel mit vielen verschiedenen Mods, die die Manipulation von Blöcken und Entities ermöglichen (z.B. mittels des [Adapters](block/adapter.md) oder für gewisse Upgrades in einem Roboter oder einer Drohne). Strom kann mit einer großen Palette von Mods zur Verfügung gestellt werden, darunter Redstone Flux, IndustrialCraft2 EU, Mekanism Joules, Applied Energistics 2-Energie sowie Factorization Charge.

Geräte in OpenComputer haben Extra-Funktionen sowie einige Einschränkungen. [Computer](general/computer.md) sind die Grundlinie, und sind in der Lage eine ordentliche Anzahl von Komponenten zu verwenden, was von der CPU des Computers abhängt. [Computer](general/computer.md) haben außerdem Zugriff auf Komponenten an allen sechs Seiten. [Server](item/server1.md) sind durch [Komponentenschnittstellen](item/componentBus1.md) in der Lage, mehr Komponenten (intern oder extern) anzusprechen als [Computer](general/computer.md), allerdings wird die Anzahl der nutzbaren Seiten durch den [Serverschrank](block/serverRack.md) beschränkt. Welche Seite das ist kann in der GUI des [Serverschranks](block/serverRack.md) konfiguriert werden. Mikrocontroller sind im Vergleich zu [Computern](general/computer.md) weiter eingeschränkt, da sie keine [Festplatte](item/hdd1.md) oder ein [Diskettenlaufwerk](block/diskDrive.md) verwenden können. Dadurch kann [OpenOS](general/openOS.md) nicht auf einem Mikrocontroller installiert werden. Allerdings haben [Mikrocontroller] einen Slot für einen [EEPROM](item/eeprom.md) und kann daher mit einem spezielleren Betriebssystem für eine begrenzte Anzahl an Aufgaben programmiert werden.

Roboter sind [Computer](general/computer.md), welche sich bewegen können. Sie können mit der Welt (aber nicht mit externen OpenComputers-Blöcken) interagieren. Im Gegensatz zu einem [Computer](general/computer.md) können die Komponenten in einem Roboter nicht mehr entfernt werden. Um diese Einschränkung zu umgehen, können Roboter mit dem [Upgrade Container](item/upgradeContainer1.md) oder einem [Card Container](item/cardContainer1.md) gebaut werden. Diese Upgrades ermöglichen einen sofortigen Austausch von Karten oder Upgrades, wenn dies benötigt wird. [OpenOS](general/openOS.md) kann auf einem Roboter installiert werden, indem ein [Diskettenlaufwerk](block/diskDrive.md) in einem Containerslot platziert wird, was den Einsatz einer [Diskette](item/floppy.md) ermöglicht. Außerdem kann man eine [Festplatte](item/hdd1.md) mit [OpenOS](general/openOS.md) in einem der [Festplatten](item/hdd1.md)slots einlegen. Um einen Roboter komplett neu zu konfigurieren, muss er [demontiert](block/disassembler.md) werden. Drohnen sind weiter eingeschränkte Roboter. Sie bewegen sich anders, können weniger Items halten und es mangelt an einem Betriebssystem. Ähnlich, wie es bei Mikrocontrollern der Fall ist, können Drohnen mit einem programmierten [EEPROM](item/eeprom.md) mit einem geringen Repertoire an Aufgaben ausgestattet werden. Die meisten Upgrades und Komponenten sind für Roboter und Drohnen gleich, allerdings verhalten sie sich unterschiedlich in Drohnen, so wie [Inventar-Upgrades](item/inventoryUpgrade.md), welche nur 4 Slots pro Upgrade zur Verfügung stellen (und damit die maximale Anzahl auf 8 begrenzen), während Roboter bis zu 4 [Inventar-Upgrades](item/inventoryUpgrade.md) aufnehmen können und mehr Slots pro Upgrade zur Verfügung stellen (insgesamt 16 Stück pro Upgrade).

Diese Bedienungsanleitung enthält detaillierte Informationen über alle Blöcke und Items, wie man unterschiedliche Systeme und Geräte aufsetzt und außerdem eine Einführung in die Lua-Programmierung.
