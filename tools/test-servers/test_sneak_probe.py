import unittest
from sneak_probe import summarize, total, sequence


class SneakingEvidenceTest(unittest.TestCase):
    def sample(self):
        player=dict(name='probe',protocol=776,gamemode='SURVIVAL',disabled=False,op=False,
                    exempt_permission=False,verbose=True,nosetback_permission=False,nomodifypacket_permission=False)
        state=dict(state='enabled',players=[player])
        case=dict(case='Sneak walk / 2',action='forward',start=1,end=3,before=state,after=state)
        ticks=[dict(time_ms=1000+i*50,phase='END',action='forward',water=False,height=1.8,loaded=True,paused=False,
                    player_x=0,player_y=64,player_z=i*.15,player_alive=True) for i in range(40)]
        events=[dict(time=1+i*.05,player='probe',type='prediction',offset=0) for i in range(40)]
        return case,ticks,events

    def test_real_dry_movement_required(self):
        c,p,e=self.sample();self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        for fault in ('water','missing_water','input','motion','prediction','creative','off','bypass','protocol','unloaded','paused'):
            c,p,e=self.sample()
            if fault=='prediction':e=[]
            if fault=='creative':c['before']['players'][0]['gamemode']='CREATIVE'
            if fault=='off':c['after']['state']='disabled'
            if fault=='bypass':c['after']['players'][0]['exempt_permission']=True
            if fault=='protocol':c['after']['players'][0]['protocol']=47
            for t in p:
                if fault=='water':t['water']=True
                if fault=='missing_water':t.pop('water')
                if fault=='input':t['action']='idle'
                if fault=='motion':t['player_z']=0
                if fault=='unloaded':t['loaded']=False
                if fault=='paused':t['paused']=True
            self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'],fault)

    def test_stopped_at_wall_requires_observed_contact_and_predictions(self):
        c,p,e=self.sample();c.update(case='Wall contact / 2',action='forward-sprint-sneak')
        for t in p:t.update(player_z=11.7,action=c['action'],collision_h=True)
        self.assertEqual('PASS',summarize(c,p,e[:2],'probe',776)['result'])
        self.assertEqual('FAIL',summarize(c,p,[],'probe',776)['result'])
        for t in p:t['collision_h']=False
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])

    def test_forced_pose_must_be_observed_without_shift(self):
        for label,height in [('Low ceiling / 2',1.5),('Crawl water exit / 2',.6)]:
            c,p,e=self.sample();c['case']=label
            for t in p:t.update(height=height,sneaking=False)
            self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
            for t in p:t['sneaking']=True
            self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])

    def test_flags_in_transition_intervals_cannot_be_hidden(self):
        report=dict(completed=True,cases=[dict(case='Survival settle',start=1),dict(case='Walk',start=3,result='PASS')])
        self.assertEqual('FAIL',total(report,[dict(type='flag',time=2)])['result'])

    def test_sequence_retains_single_tick_edges(self):
        self.assertEqual('sequence:20:forward|1:forward-sneak|3:forward|60:idle',
                         sequence([(20,'forward'),(1,'forward-sneak'),(3,'forward'),(60,'idle')]))

    def test_duplicate_sprint_actions_fail(self):
        report=dict(completed=True,cases=[dict(case='Survival settle',start=1),dict(case='Walk',start=3,result='PASS')])
        events=[dict(type='packet',time=2,action='START_SPRINTING'),dict(type='packet',time=3,action='STOP_SPRINTING')]
        self.assertEqual('PASS',total(report,events)['result'])
        self.assertEqual('FAIL',total(report,events+[events[-1]])['result'])

    def test_zero_speed_requires_finite_stationary_movement_and_real_attribute(self):
        c,p,e=self.sample();c.update(case='Zero sneak speed',action='forward-right-sneak')
        for t in p:t.update(action=c['action'],sneak_speed=0,player_z=0,player_vx=0,player_vy=0,player_vz=0)
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        p[-1]['player_x']=float('nan')
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])
