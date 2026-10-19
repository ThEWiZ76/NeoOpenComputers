"""Inventory source surfaces, without treating matching names as implemented parity."""

import argparse
from collections import defaultdict
import json
from pathlib import Path
import re


def callbacks(path):
    source = path.read_text(encoding="utf-8")
    annotations = list(re.finditer(r"@Callback\b", source))
    result = []
    method = (r"\bdef\s+(\w+)\s*\(" if path.suffix == ".scala" else
              r"\bpublic\s+(?:(?:final|static|synchronized|default|abstract)\s+)*[\w.$<>?\[\]]+\s+(\w+)\s*\(")
    for index, annotation in enumerate(annotations):
        end = annotations[index + 1].start() if index + 1 < len(annotations) else len(source)
        declaration = re.search(method, source[annotation.end():end])
        if declaration is None:
            raise ValueError(f"Unparsed callback at {path}:{source.count(chr(10), 0, annotation.start()) + 1}")
        position = annotation.end() + declaration.start()
        attributes = source[annotation.end():position]
        alias = re.search(r'\bvalue\s*=\s*"([^"]+)"', attributes)
        positional = re.match(r'\s*\(\s*"([^"]+)"', attributes)
        result.append({"name": (alias or positional).group(1) if alias or positional else declaration.group(1),
                       "line": source.count("\n", 0, position) + 1})
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--upstream", type=Path, required=True)
    parser.add_argument("--upstream-java", type=Path, required=True, help="Git export root containing src/main/java")
    parser.add_argument("--baseline-revision", required=True)
    parser.add_argument("--port-revision", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    upstream = args.upstream / "src/main/scala/li/cil/oc"
    port = root / "src/main/java/li/cil/oc"
    port_index = defaultdict(list)
    for path in sorted(port.rglob("*.java")):
        if "gametest" in path.parts:
            continue
        for callback in callbacks(path):
            port_index[callback["name"]].append({"path": path.relative_to(root).as_posix(), "line": callback["line"]})
    domains = {"blocks": "common/block", "items": "common/item", "tiles": "common/tileentity",
               "components_and_traits": "server/component", "machine": "server/machine", "integrations": "integration"}
    entries = []
    for domain, relative in domains.items():
        for path in sorted((upstream / relative).rglob("*")):
            if path.suffix not in {".scala", ".java"}:
                continue
            declarations = callbacks(path)
            entries.append({"domain": domain, "path": path.relative_to(args.upstream).as_posix(),
                            "callbacks": [{**item, "same_name_port_declarations": port_index[item["name"]]} for item in declarations]})
    upstream_api = args.upstream_java / "src/main/java/li/cil/oc/api"
    if not upstream_api.is_dir():
        raise SystemExit("Missing API export; do not silently inventory a sparse checkout as complete")
    api = [{"path": path.relative_to(args.upstream_java).as_posix(),
            "same_path_port_file": (root / path.relative_to(args.upstream_java)).is_file()}
           for path in sorted(upstream_api.rglob("*.java"))]
    missing = defaultdict(list)
    for entry in entries:
        for item in entry["callbacks"]:
            if not item["same_name_port_declarations"]:
                missing[item["name"]].append(f'{entry["path"]}:{item["line"]}')
    report = {"baseline_revision": args.baseline_revision, "port_revision": args.port_revision,
              "limitations": "Lexical declared callback names only. Global name matches do not establish the right host, inheritance, registration, behavior, arguments, energy, events or tests. Missing names are review candidates, not confirmed defects. API path matches do not establish signatures or addon compatibility. Assets, recipes, config and live acceptance remain separate inventories.",
              "source_files": entries, "api_files": api, "names_not_found_by_scanner": dict(sorted(missing.items()))}
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / "PORT_SURFACE_INVENTORY.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    lines = ["# Port source surface inventory", "", f"Baseline: `{args.baseline_revision}`. Port source: `{args.port_revision}`.", "", report["limitations"], "",
             "The baseline Java API was exported read-only from Git because its sparse working checkout omits Java. The full file/declaration crosswalk is PORT_SURFACE_INVENTORY.json.", "", "| Domain | Upstream source files | Declared callbacks |", "| --- | ---: | ---: |"]
    for domain in domains:
        selected = [entry for entry in entries if entry["domain"] == domain]
        lines.append(f'| {domain} | {len(selected)} | {sum(len(entry["callbacks"]) for entry in selected)} |')
    lines += ["", f"Java API files: {len(api)}. Missing matching port paths: {sum(not item['same_path_port_file'] for item in api)}.", "", "## Callback names requiring source review", "", "| Name not found in annotated port declarations | Upstream declaration(s) |", "| --- | --- |"]
    for name, paths in sorted(missing.items()):
        lines.append(f"| {name} | {'; '.join(paths)} |")
    lines += ["", "## API paths requiring source review", ""]
    lines += [f'- {item["path"]}' for item in api if not item["same_path_port_file"]]
    (args.output / "PORT_SURFACE_INVENTORY.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({"files": len(entries), "api_files": len(api), "callback_names_for_review": list(sorted(missing))}))


if __name__ == "__main__":
    main()
