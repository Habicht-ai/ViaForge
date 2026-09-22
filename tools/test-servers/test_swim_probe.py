import unittest
from swim_probe import summarize,total

class SwimmingEvidenceTest(unittest.TestCase):
    def sample(self):
        player=dict(name='probe',protocol=404,gamemode='SURVIVAL',disabled=False,op=False,
                    exempt_permission=False,verbose=True,nosetback_permission=False,nomodifypacket_permission=False)
        state=dict(state='enabled',players=[player]);case=dict(case='Sprint swim level',action='forward-sprint',start=1,end=3,before=state,after=state)
        ticks=[dict(time_ms=1000+i*50,phase='END',action='forward-sprint',water=True,swimming=True,
                    player_x=0,player_y=63,player_z=i*.15,player_alive=True) for i in range(40)]
        events=[dict(time=1+i*.05,player='probe',type='prediction',offset=0) for i in range(40)]
        return case,ticks,events
    def test_genuine_swimming(self):
        c,p,e=self.sample();self.assertEqual('PASS',summarize(c,p,e,'probe',404)['result'])
    def test_no_flags_alone_is_insufficient(self):
        for change in ['upright','no_water','no_input','no_motion','no_prediction','creative','disabled']:
            c,p,e=self.sample()
            if change=='no_prediction':e=[]
            if change=='creative':c['before']['players'][0]['gamemode']='CREATIVE'
            if change=='disabled':c['before']['state']='disabled'
            for tick in p:
                if change=='upright':tick['swimming']=False
                if change=='no_water':tick['water']=False
                if change=='no_input':tick['action']='idle'
                if change=='no_motion':tick['player_z']=0
            self.assertEqual('FAIL',summarize(c,p,e,'probe',404)['result'],change)
    def test_flags_or_setbacks_fail(self):
        for kind in ['flag','setback']:
            c,p,e=self.sample();e.append(dict(type=kind,player='probe',time=2))
            self.assertEqual('FAIL',summarize(c,p,e,'probe',404)['result'])
    def test_unapplied_effect_or_equipment_cannot_pass(self):
        for label in ['Dolphins grace','Depth strider swimming','Conduit breathing']:
            c,p,e=self.sample();c['case']=label
            self.assertEqual('FAIL',summarize(c,p,e,'probe',404)['result'],label)
    def test_early_shift_start_is_not_a_modern_swimming_pose(self):
        c,p,e=self.sample();c.update(case='Dive while swimming',action='forward-sprint-sneak')
        for t in p:t.update(action='forward-sprint-sneak',swimming=False)
        self.assertEqual('PASS',summarize(c,p,e,'probe',404)['result'])
        c['before']['players'][0]['protocol']=776
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])
    def test_equipment_reset_flag_prevents_overall_pass(self):
        report=dict(completed=True,cases=[dict(case='Creative login',start=0),dict(case='Survival settle',start=1),dict(case='Sprint',start=3,result='PASS')])
        evidence=total(report,[dict(type='setback',time=.5),dict(type='flag',time=2)])
        self.assertEqual('FAIL',evidence['result']);self.assertEqual(1,len(evidence['login_issues']));self.assertEqual(1,len(evidence['survival_issues']))
    def test_passage_must_be_reached_and_exited(self):
        c,p,e=self.sample();c['case']='Swim into low passage'
        for tick in p:tick['player_y']=61
        self.assertEqual('PASS',summarize(c,p,e,'probe',404)['result'])
        for tick in p:tick['player_z']+=20
        self.assertEqual('FAIL',summarize(c,p,e,'probe',404)['result'])
        c['case']='Swim out of low passage'
        self.assertEqual('PASS',summarize(c,p,e,'probe',404)['result'])
        for tick in p:tick['player_z']-=20
        self.assertEqual('FAIL',summarize(c,p,e,'probe',404)['result'])
