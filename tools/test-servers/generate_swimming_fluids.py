"""Generate fluid facts from the exact Mojang server block-state reports (no fallback mappings)."""
import hashlib
import json
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
ALIASES = {'1.16.5':'1.16.4', '1.18.1':'1.18', '1.19.2':'1.19.1',
           '1.20.1':'1.20', '1.20.4':'1.20.3', '1.20.6':'1.20.5',
           '1.21.1':'1.21', '1.21.3':'1.21.2', '1.21.8':'1.21.7',
           '1.21.10':'1.21.9', '26.1.2':'26.1'}

def descriptor(name, properties):
    if name == 'minecraft:water': return 1 + int(properties['level'])
    if name == 'minecraft:bubble_column': return 18 if properties['drag'] == 'true' else 17
    if properties.get('waterlogged') == 'true' or name in ('minecraft:kelp', 'minecraft:kelp_plant', 'minecraft:seagrass', 'minecraft:tall_seagrass'): return 1
    return 0

def generate():
    result = {}
    rows = json.loads((REPO/'tools/test-servers/versions.json').read_text())
    for row in rows:
        if row['protocol'] < 393: continue
        version = row['version']
        path = REPO/'run/test-servers'/version/'data/reports/blocks.json'
        raw = path.read_bytes()
        states = {s['id']:descriptor(name,s.get('properties',{})) for name,b in json.loads(raw).items() for s in b['states']}
        assert set(states) == set(range(len(states))), version
        runs = []
        for i in range(len(states)):
            value = states[i]
            if not value: continue
            if runs and runs[-1][1]+1 == i and runs[-1][2] == value: runs[-1][1] = i
            else: runs.append([i,i,value])
        entry = dict(version=version,protocol=row['protocol'],server=row['server'],
                     report_sha256=hashlib.sha256(raw).hexdigest(),states=len(states),fluids=runs)
        result[version] = entry
    # Wire-equivalent patch names used by Via's declared translation boundaries.
    # They are aliases of the documented source report, never claims of a separate server test.
    for source,alias in ALIASES.items():result[alias] = result[source]
    return result

if __name__ == '__main__':
    target = REPO/'src/main/resources/assets/viaforge/fluid-states.json'
    target.write_text(json.dumps(generate(),separators=(',',':'),sort_keys=True)+'\n',encoding='utf-8')
    print(target)
