import unittest
from edge_probe import summarize
import test_sneak_probe


class EdgeEvidenceTest(unittest.TestCase):
    def sample(self):
        return test_sneak_probe.SneakingEvidenceTest().sample()

    def test_landing_requires_flight_before_ground_contact(self):
        c,p,e=self.sample();c.update(case='Edge flight ground jump')
        for i,t in enumerate(p):t.update(elytra=5<=i<15,ground=i>=15)
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        for t in p:t.update(ground=False)
        self.assertIn('No actual landing',summarize(c,p,e,'probe',776)['reasons'])
        for t in p:t.update(elytra=False,ground=True)
        self.assertIn('No actual low flight',summarize(c,p,e,'probe',776)['reasons'])

    def test_swimming_coordinates_do_not_prove_a_bubble_column(self):
        c,p,e=self.sample();c.update(case='Edge bubble lift forward-sprint')
        for t in p:t.update(swimming=True,player_y=64,player_z=10,bubble=0)
        self.assertIn('Actual column direction not observed',summarize(c,p,e,'probe',776)['reasons'])
        for t in p:t['bubble']=18
        self.assertIn('Actual column direction not observed',summarize(c,p,e,'probe',776)['reasons'])
        for i,t in enumerate(p):t.update(bubble=17,player_z=10+i*.1)
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])

    def test_single_open_does_not_prove_repeated_shulker_cycles(self):
        c,p,e=self.sample();c.update(case='Edge shulker fast')
        for i,t in enumerate(p):t['shulker_progress']=min(i,10)/10
        self.assertIn('Repeated original opening/closing not observed',summarize(c,p,e,'probe',776)['reasons'])
        for i,t in enumerate(p):t['shulker_progress']=(i%4 if i%4<2 else 4-i%4)*.1
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
