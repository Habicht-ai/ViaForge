import unittest
from interaction_probe import total, summarize, attack_interleavings
import test_sneak_probe


class InteractionEvidenceTest(unittest.TestCase):
    def test_delayed_swing_fails_even_when_filtered_attack_order_looks_correct(self):
        packets=[dict(packet=k) for k in ('ATTACK','PLAYER_INPUT','ANIMATION')]
        self.assertEqual(['PLAYER_INPUT'],[p['packet'] for p in attack_interleavings(packets)])
        self.assertEqual([],attack_interleavings([packets[0],packets[2],packets[1]]))
        self.assertEqual([],attack_interleavings([packets[0],dict(packet='PONG'),packets[2]]))
        self.assertEqual(['MISSING_SWING'],[p['packet'] for p in attack_interleavings(packets[:1])])

    def test_zero_sequences_fail_even_with_experimental_check_off(self):
        report=dict(completed=True,cases=[dict(case='Survival settle',start=1),dict(case='Use',result='PASS')])
        events=[dict(type='packet',time=2,packet='USE_ITEM',sequence=0)]
        self.assertEqual('FAIL',total(report,events)['result'])
        events[0]['sequence']=1
        self.assertEqual('PASS',total(report,events)['result'])

    def test_use_and_dig_share_sequence_but_release_and_abort_do_not_advance(self):
        report=dict(completed=True,cases=[dict(case='Survival settle',start=1),dict(case='Use',result='PASS')])
        events=[dict(type='packet',time=2,packet=p,sequence=n,action=a) for p,n,a in [
            ('USE_ITEM',1,None),('PLAYER_DIGGING',0,'RELEASE_USE_ITEM'),
            ('PLAYER_BLOCK_PLACEMENT',2,None),('PLAYER_DIGGING',3,'START_DIGGING'),
            ('PLAYER_DIGGING',0,'CANCELLED_DIGGING'),('PLAYER_DIGGING',4,'START_DIGGING'),
            ('PLAYER_DIGGING',5,'FINISHED_DIGGING')]]
        self.assertEqual('PASS',total(report,events)['result'])
        events[-1]['sequence']=6
        self.assertEqual('FAIL',total(report,events)['result'])

    def test_no_prediction_or_missing_input_is_not_a_movement_pass(self):
        c,p,e=test_sneak_probe.SneakingEvidenceTest().sample();c['case']='Walk control'
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        self.assertEqual('FAIL',summarize(c,p,[],'probe',776)['result'])
        for t in p:t['action']='idle'
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])

    def test_reset_flags_are_counted(self):
        report=dict(completed=True,cases=[dict(case='Survival settle',start=1),dict(case='Use',result='PASS')])
        self.assertEqual('FAIL',total(report,[dict(type='flag',time=2,check='NoSlow')])['result'])

    def test_rightclick_turn_requires_camera_motion_and_real_target_packets(self):
        c,p,e=test_sneak_probe.SneakingEvidenceTest().sample()
        c.update(case='Rightclick Stick ground turn',action='use-turn')
        for i,t in enumerate(p):t.update(action='use-turn',yaw=float(i))
        e.extend([dict(type='packet',time=(c['start']+c['end'])/2,packet=k) for k in ('USE_ITEM','PLAYER_BLOCK_PLACEMENT')])
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        for t in p:t['yaw']=0
        self.assertIn('Camera turn not observed',summarize(c,p,e,'probe',776)['reasons'])
        e=[row for row in e if row.get('packet')!='PLAYER_BLOCK_PLACEMENT']
        self.assertIn('Ground block was not targeted',summarize(c,p,e,'probe',776)['reasons'])

    def test_grim_clean_rejected_placement_still_requires_original_packet_flow(self):
        c,p,e=test_sneak_probe.SneakingEvidenceTest().sample()
        c.update(case='Rightclick Blocks ground turn',action='use-turn')
        for i,t in enumerate(p):t.update(action='use-turn',yaw=float(i))
        place=dict(type='packet',time=(c['start']+c['end'])/2,packet='PLAYER_BLOCK_PLACEMENT',hand='MAIN_HAND')
        e.append(place)
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        for extra in [dict(place,packet='USE_ITEM'),dict(place,hand='OFF_HAND')]:
            self.assertIn('Rejected placement incorrectly falls through to air use or offhand',summarize(c,p,e+[extra],'probe',776)['reasons'])
