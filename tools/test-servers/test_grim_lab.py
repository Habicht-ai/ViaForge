import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import lab
import grim
import snapshots
import terrain
import world_audit


class SnapshotTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)
        self.patch=patch.object(lab,'ROOT',self.root);self.patch.start();self.addCleanup(self.patch.stop)
        self.stop=patch.object(snapshots,'assert_stopped');self.stop.start();self.addCleanup(self.stop.stop)
        self.row=lab.select('1.12.2-grim')[0]
        self.folder=self.root/self.row['version'];(self.folder/'world/playerdata').mkdir(parents=True)
        (self.folder/'world/playerdata/existing.dat').write_bytes(b'original inventory and mode')
        (self.folder/'server.properties').write_text('gamemode=1\nforce-gamemode=false\n')

    def test_restore_keeps_previous_inventory_and_both_dimensions(self):
        for name in ['world_nether','world_the_end']:
            (self.folder/name).mkdir();(self.folder/name/'level.dat').write_bytes(name.encode())
        saved=snapshots.backup(self.row)
        player=self.folder/'world/playerdata/existing.dat';player.write_bytes(b'new inventory')
        snapshots.restore(self.row,saved.name)
        self.assertEqual(b'original inventory and mode',player.read_bytes())
        result=json.loads((self.folder/'snapshot-restore.json').read_text())
        self.assertEqual(b'new inventory',(Path(result['before'])/'world/playerdata/existing.dat').read_bytes())
        self.assertTrue((self.folder/'world_the_end/level.dat').exists())

    def test_tampered_and_unhashed_files_refused_before_mutation(self):
        saved=snapshots.backup(self.row)
        file=saved/'world/playerdata/existing.dat';file.write_bytes(b'tampered')
        with self.assertRaises(ValueError):snapshots.restore(self.row,saved.name)
        file.write_bytes(b'original inventory and mode')
        (saved/'world/unverified').write_text('unverified')
        with self.assertRaises(ValueError):snapshots.restore(self.row,saved.name)
        self.assertEqual(b'original inventory and mode',(self.folder/'world/playerdata/existing.dat').read_bytes())

    def test_traversal_and_wrong_instance_rejected(self):
        with self.assertRaises(ValueError):snapshots.restore(self.row,'../world')
        saved=snapshots.backup(self.row);file=saved/'snapshot.json';data=json.loads(file.read_text());data['version']='26.2-grim';file.write_text(json.dumps(data))
        with self.assertRaises(ValueError):snapshots.restore(self.row,saved.name)


class GrimStateTests(unittest.TestCase):
    def test_dead_heartbeat_cannot_claim_checks_active(self):
        row=lab.select('1.12.2-grim')[0]
        with tempfile.TemporaryDirectory() as tmp,patch.object(lab,'ROOT',Path(tmp)):
            file=Path(tmp)/row['version']/'plugins/ViaForgeLabAC/status.json';file.parent.mkdir(parents=True)
            file.write_text(json.dumps(dict(time=1,state='enabled')))
            self.assertEqual('unavailable',grim.state(row,True)['state'])
            self.assertEqual('offline',grim.state(row,False)['state'])

    def test_26_3_never_has_a_substituted_grim_variant(self):
        self.assertNotIn('26.3',[r['minecraft_version'] for r in lab.GRIM_VERSIONS])
        for row in lab.GRIM_VERSIONS:
            reference=lab.select(row['reference_version'])[0]
            self.assertEqual(reference['protocol'],row['protocol'])
            self.assertNotEqual(reference['port'],row['port'])

    def test_failed_plugin_start_without_heartbeat_is_unavailable(self):
        row=lab.select('1.13-grim')[0]
        with tempfile.TemporaryDirectory() as tmp,patch.object(lab,'ROOT',Path(tmp)):
            file=Path(tmp)/row['version']/'grim-startup-probe.json';file.parent.mkdir(parents=True)
            file.write_text(json.dumps(dict(success=False,error='Command initialization failed')))
            actual=grim.state(row,False)
            self.assertEqual('unavailable',actual['state'])
            self.assertIn('Command',actual['reason'])

    def test_missing_region_is_reported_as_missing_chunk(self):
        with tempfile.TemporaryDirectory() as tmp:
            world=world_audit.World(Path(tmp))
            self.assertEqual({},world.chunk(0,0))

    def test_terrain_above_old_ceiling_is_detected_with_specimen_preserved(self):
        class World:
            def chunk(self,x,z):return {'sections':[{'Y':18,'block_states':{'palette':['minecraft:oak_leaves']}}]} if (x,z)==(0,0) else {}
            def block(self,x,y,z):return {'Name':'minecraft:oak_leaves' if (x,y,z) in {(2,295,2),(3,295,2)} else 'minecraft:air'}
            def block_entity(self,*args):return {}
        self.assertEqual([(3,295,2)],list(terrain.intrusion_positions(World(),{(2,295,2)})))

    def test_fluid_barrier_stays_inside_and_preserves_specimen(self):
        class World:
            def block(self,x,y,z):return {'Name':'minecraft:water' if (x,y,z) in {(80,70,1),(80,70,2)} else 'minecraft:air'}
            def block_entity(self,*args):return {}
        self.assertEqual(['setblock 79 70 1 glass'],terrain.boundary_commands(World(),{(79,70,2)}))


if __name__=='__main__':unittest.main()
