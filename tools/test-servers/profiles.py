"""One public Minecraft version per lab world; historical instances stay addressable."""
import json
import lab


def entries():
    rows = json.loads((lab.HERE / 'profiles.json').read_text(encoding='utf-8'))
    instances = {r['version']: r for r in lab.VERSIONS}
    originals = {r['version']: r for r in lab.VANILLA_VERSIONS}
    if len(rows) != len(originals) or {r['version'] for r in rows} != set(originals):
        raise ValueError('Profiles must contain exactly one entry per original Minecraft version')
    if len({r['instance'] for r in rows}) != len(rows):
        raise ValueError('A world cannot belong to two public profiles')
    for row in rows:
        actual = instances[row['instance']]
        if actual.get('minecraft_version', actual['version']) != row['version'] or actual['protocol'] != originals[row['version']]['protocol']:
            raise ValueError('Profile must use the exact Minecraft version: ' + row['version'])
    return rows


def mapping():
    instances = {r['version']: r for r in lab.VERSIONS}
    return {p['version']: dict(instances[p['instance']], profile_version=p['version'],
                             unavailable_reason=p.get('unavailable_reason')) for p in entries()}


def select(value='all'):
    # Explicit historical IDs remain available for recovery and developer scripts.
    if value.startswith('instance:'):
        return lab.select(value[len('instance:'):])
    if value in ('vanilla', 'grim') or '-grim' in value:
        return lab.select(value)
    rows = mapping()
    groups = {'regression': ['1.12.2', '1.13.2', '1.21.11', '26.3'],
              'legacy': ['1.9', '1.10.2', '1.11.2', '1.12.2'],
              'modern': ['1.16.5', '1.18.2', '1.20.6', '26.3']}
    names = list(rows) if value == 'all' else groups.get(value, value.split(','))
    if len(names) != len(set(names)) or any(n not in rows for n in names):
        raise ValueError('Unbekannte Version/Gruppe: ' + value)
    return [rows[n] for n in names]
