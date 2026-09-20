import unittest
import tempfile
import zipfile
import gzip
from pathlib import Path
from unittest.mock import patch
import arena
import lab
import terrain
import world_audit
from server_list import utf


class TerrainTests(unittest.TestCase):
    def test_bootstrap_has_real_flat_nbt_and_never_changes_existing_world(self):
        row=lab.select('1.13.2')[0]
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab,'ROOT',Path(tmp)):
            folder=Path(tmp)/row['version'];folder.mkdir()
            with zipfile.ZipFile(folder/'server.jar','w'): pass
            self.assertTrue(terrain.prepare_new_world(row))
            level=folder/'world/level.dat';before=level.read_bytes()
            data=world_audit.nbt(gzip.decompress(before))['Data']
            self.assertEqual(1631,data['DataVersion'])
            self.assertEqual(terrain.flat_settings(row)['layers'],data['generatorOptions']['layers'])
            self.assertEqual(1,data['GameType'])
            self.assertFalse(terrain.prepare_new_world(row))
            self.assertEqual(before,level.read_bytes())

    def test_26_3_compact_block_palette_is_read_as_original_name(self):
        world=world_audit.World.__new__(world_audit.World)
        world.chunk=lambda x,z: {'sections':[{'Y':0,'block_states':{'palette':['minecraft:stone']}}]}
        self.assertEqual({'Name':'minecraft:stone'},world.block(3,5,7))
        self.assertTrue(terrain.natural('minecraft:stone'))
        self.assertEqual({'Name':'minecraft:oak_sign'},world_audit.block_state({'':'minecraft:oak_sign'}))
        self.assertEqual({'Name':'minecraft:water','Properties':{'level':'1'}},world_audit.block_state({'id':'minecraft:water','properties':{'level':'1'}}))

    def test_generator_change_preserves_unrelated_nbt_bytes_and_types(self):
        # Unknown long, byte-array and player inventory survive byte-for-byte.
        other = b'\x04' + utf('seed') + b'\xff' * 8 + b'\x07' + utf('inventory') + b'\0\0\0\3abc'
        raw = b'\x0a\0\0\x0a' + utf('Data') + other + b'\x08' + utf('generator') + utf('noise') + b'\0\0'
        out = terrain.replace_tag(raw, ['Data', 'generator'], {'type': 'minecraft:flat'})
        self.assertIn(other, out)
        self.assertEqual('minecraft:flat', world_audit.nbt(out)['Data']['generator']['type'])
        with self.assertRaises(ValueError): terrain.replace_tag(raw, ['missing'], {})

    def test_real_parser_boundaries_and_shared_ground_height(self):
        for version in ['1.9', '1.13.2', '1.15.2', '1.16.1', '1.18.2', '1.19', '26.3']:
            row = lab.select(version)[0]
            text = terrain.properties('view-distance=16\nsimulation-distance=3\n', row)
            self.assertIn('level-type=' + ('minecraft:flat' if row['protocol'] >= 759 else 'flat'), text)
            self.assertIn('view-distance=16\nsimulation-distance=3', text)
            self.assertEqual(128 if row['protocol'] >= 757 else 64, sum(x['height'] for x in terrain.flat_settings(row)['layers']))
            if 393 <= row['protocol'] < 735:
                self.assertIn('generator-settings=minecraft:bedrock,59*minecraft:stone', text)

    def test_extra_terrain_is_detected_without_touching_exhibit_or_outside(self):
        class World:
            def chunk(self, x, z):
                return {'sections': [{'Y': 4, 'block_states': {'palette': [{'Name': 'minecraft:oak_log'}]}}]} if (x,z) == (0,0) else {}
            def block(self, x, y, z):
                return {'Name': 'minecraft:oak_log' if (x,y,z) in {(2,65,2),(3,66,2)} else 'minecraft:air'}
            def block_entity(self, *pos): return {}
        self.assertEqual([(3,66,2)], list(terrain.intrusion_positions(World(), {(2,65,2)})))
        self.assertEqual(['fill 2 65 2 3 65 2 air'], terrain.removal_commands([(2,65,2),(3,65,2)]))
        with self.assertRaises(ValueError): terrain.removal_commands([(80,65,2)])

    def test_station_fireworks_and_modes_are_versioned_and_explicit(self):
        for version in ['1.9','1.11','1.11.2','26.3']:
            row=lab.select(version)[0]
            with patch.object(lab,'catalog',return_value={'blocks': [], 'items': [], 'mobs': []}):
                a=arena.Arena(row); a.elytra()
            text='\n'.join(a.commands)
            self.assertEqual(row['protocol']>=316, 'give @p minecraft:firework' in text)
            self.assertNotIn('gamemode adventure', text)
            self.assertNotIn('gamemode 2', text)
            self.assertTrue(any('Elytra anziehen' in c for c in a.commands))


if __name__ == '__main__': unittest.main()
