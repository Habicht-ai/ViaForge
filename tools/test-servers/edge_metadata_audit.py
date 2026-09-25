"""Correlate completed edge-probe flight-end metadata with Pong and player ticks.

The isolated single-player fixtures recorded entity ID 1; pass the actual ID
explicitly when inspecting another fixture. Reads completed logs only.
"""
import argparse,json,pathlib,re,hashlib
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('reports',nargs='+',help='Completed run directory names under run/test-servers')
parser.add_argument('--entity-id',type=int,default=1)
args=parser.parse_args()
for name in args.reports:
 root=pathlib.Path('run/test-servers')/name
 report=json.loads((root/'report.json').read_text())
 assert report['completed'], 'Only completed reports may be audited'
 kind='native' if report['client']=='native' else 'viaforge'
 paths={n:root/kind/n for n in ['packet-order.jsonl','client.jsonl']}
 rows=[json.loads(s) for s in paths['packet-order.jsonl'].read_text().splitlines()]
 ticks=[json.loads(s) for s in paths['client.jsonl'].read_text().splitlines()]
 ticks=[t for t in ticks if t.get('phase')=='END']
 clears=[];previous=0
 for i,r in enumerate(rows):
  if kind=='native':
   if r['phase']!='handleSetEntityData_END' or r.get('id')!=args.entity_id:continue
   m=re.search(r'DataValue\[id=0,.*?value=(-?\d+)\]',r.get('metadata',''))
   if not m:continue
   flags=int(m[1]);pongs=[p for p in rows[:i] if p['phase']=='send_BEGIN' and p['packet']=='ServerboundPongPacket']
  else:
   if r['phase']!='CUSTOM_RECEIVE' or r.get('thread')!='Client thread' or r.get('channel')!='VF|entity':continue
   b=bytes.fromhex(r['bytes'])
   if len(b)<8 or b[:3]!=bytes.fromhex('015402'):continue
   from metadata_relay import varint
   entity_id,offset=varint(b,3)
   if entity_id!=args.entity_id or b[offset:offset+2]!=b'\0\0':continue
   flags=b[offset+2];pongs=[p for p in rows[:i] if p['phase']=='CUSTOM_SEND' and p.get('channel')=='VF|pong']
  was_flying=bool(previous&128);previous=flags
  if not was_flying or flags&128 or not pongs:continue
  pong=pongs[-1]
  crossed=[{k:t.get(k) for k in ['time_ms','player_tick','elytra','action','player_y','player_z']} for t in ticks if pong['time_ms']<=t['time_ms']<r['time_ms']]
  clears.append(dict(applied=r,previous_pong=pong,intervening_ticks=crossed))
 out=dict(run=name,client=kind,flight_end_transitions=len(clears),crossed_tick=sum(bool(c['intervening_ticks']) for c in clears),transitions=clears,
          sources={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths.values()})
 target=pathlib.Path('build/inspection/flight-edges')/(name+'-metadata.json')
 target.write_text(json.dumps(out,indent=2)+'\n');print(name,len(clears),out['crossed_tick'],target)
