"""Seed external loot files only in a new, isolated world for the two-JVM probe."""
from pathlib import Path
import re
import sys

name = sys.argv[1]
assert re.fullmatch(r"oc-restart-world-loot-[a-zA-Z0-9_-]+", name), "Use an isolated world-loot restart name"
base = Path("run/gameTestServer").resolve()
world = (base / name).resolve()
assert world.parent == base and not world.exists(), "Preserve existing worlds; choose a new probe name"
loot = world / "opencomputers/loot"
(loot / "network").mkdir(parents=True)
(loot / "legacy-only").mkdir()
(loot / "loot.properties").write_text("network=World restart:3:dyeBlue\n", encoding="ascii")
(loot / "network/probe.bin").write_bytes(bytes([97, 0, 128, 255, 42]))
(loot / "legacy-only/probe.bin").write_bytes(bytes([17, 0, 255]))
print("Seeded new world-loot restart fixture:", world)
