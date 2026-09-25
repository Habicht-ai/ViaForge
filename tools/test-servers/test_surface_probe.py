import unittest
from surface_probe import summarize,fixture
import test_sneak_probe


class SurfaceEvidenceTest(unittest.TestCase):
    def test_legacy_unloaded_fixture_is_not_silently_accepted(self):
        with self.assertRaises(RuntimeError):
            fixture(lambda commands:'Cannot place blocks outside of the world',340)

    def sample(self):
        case,poses,events=test_sneak_probe.SneakingEvidenceTest().sample()
        case.update(case='Surface Slime walk')
        for tick in poses:tick['floor']='minecraft:slime_block'
        return case,poses,events

    def test_requires_loaded_slime_real_input_and_predictions(self):
        c,p,e=self.sample()
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        self.assertEqual('FAIL',summarize(c,p,[],'probe',776)['result'])
        for t in p:t['floor']='minecraft:stone'
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])
        for t in p:t.update(floor='minecraft:slime_block',action='idle')
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])

    def test_late_position_snap_is_not_original_lid_motion(self):
        c,p,e=self.sample();c.update(case='Surface Shulker east',action='use')
        for i,t in enumerate(p):t.update(action='use',player_x=0 if i<20 else 1.1,shulker_progress=1)
        self.assertIn('Progressive original lid push missing',summarize(c,p,e,'probe',776)['reasons'])
        for i,t in enumerate(p):t['player_x']=min(i,10)*.11
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])

    def test_dead_or_grounded_client_is_not_a_flight_pass(self):
        c,p,e=self.sample();c.update(case='Flight vertical unpowered',action='lookup')
        for t in p:t.update(action='lookup',elytra=True,pitch=-90)
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        for t in p:t['elytra']=False
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])
        for t in p:t.update(elytra=True,player_alive=False)
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])

    def test_air_above_build_height_still_requires_loaded_column(self):
        c,p,e=self.sample();c.update(case='Flight vertical unpowered',action='lookup')
        for t in p:t.update(action='lookup',elytra=True,pitch=-90,loaded=False,loaded_column=True,player_y=280)
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        for t in p:t['loaded_column']=False
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])
