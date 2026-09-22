import unittest
from boat_evidence import summarize


class BoatEvidenceTest(unittest.TestCase):
    def fixture(self):
        user=dict(name='probe',protocol=340,gamemode='SURVIVAL',op=False,disabled=False,verbose=True,
                  exempt_permission=False,nosetback_permission=False,nomodifypacket_permission=False)
        case=dict(case='Rest in water',action='idle',start=1,end=2,before=dict(players=[user]),after=dict(players=[user]))
        poses=[dict(time_ms=1000+i*50,phase='END',boat=1,driver=True,x=0,y=64,z=0,vx=0,vz=0,status='WATER') for i in range(20)]
        events=[dict(time=1+i*.05,player='probe',type='prediction',vehicle=True,offset=0) for i in range(20)]
        return case,poses,events

    def test_active_driver_with_predictions(self):
        c,p,e=self.fixture();self.assertEqual('PASS',summarize(c,p,e,'probe',340)['result'])

    def test_empty_chat_missing_driver_never_passes(self):
        c,p,e=self.fixture()
        for pose in p:pose.pop('boat')
        self.assertEqual('INCOMPLETE',summarize(c,p,e,'probe',340)['result'])

    def test_creative_disabled_wrong_protocol_or_no_predictions(self):
        for key,value in [('gamemode','CREATIVE'),('disabled',True),('protocol',47),('op',True),('exempt_permission',True),('verbose',False)]:
            c,p,e=self.fixture();c['before']['players'][0][key]=value
            self.assertEqual('INCOMPLETE',summarize(c,p,e,'probe',340)['result'],key)
        c,p,e=self.fixture();self.assertEqual('INCOMPLETE',summarize(c,p,[],'probe',340)['result'])

    def test_flags_and_setbacks_override_good_motion(self):
        for kind in ('flag','setback'):
            c,p,e=self.fixture();e.append(dict(time=1.5,player='probe',type=kind))
            self.assertEqual('FAIL',summarize(c,p,e,'probe',340)['result'])

    def test_other_account_and_time_not_attributed(self):
        c,p,e=self.fixture();e.extend([dict(time=1.5,player='other',type='flag'),dict(time=3,player='probe',type='flag')])
        self.assertEqual('PASS',summarize(c,p,e,'probe',340)['result'])

    def test_enabling_checks_only_after_case_is_not_pass(self):
        c,p,e=self.fixture();c['before']=dict(players=[dict(c['before']['players'][0],disabled=True)])
        self.assertEqual('INCOMPLETE',summarize(c,p,e,'probe',340)['result'])

    def test_missing_before_or_after_status_is_not_pass(self):
        for phase in ('before','after'):
            c,p,e=self.fixture();c[phase]=dict(players=[])
            self.assertEqual('INCOMPLETE',summarize(c,p,e,'probe',340)['result'])

    def test_claimed_collision_or_transition_requires_actual_evidence(self):
        for name in ('Water into solid shore','Stone to water'):
            c,p,e=self.fixture();c['case']=name
            self.assertEqual('INCOMPLETE',summarize(c,p,e,'probe',340)['result'])

    def test_second_passenger_must_not_send_vehicle_move(self):
        c,p,e=self.fixture();c['case']='Second player passenger'
        for pose in p:pose['driver']=False
        self.assertEqual('PASS',summarize(c,p,e,'probe',340)['result'])
        e.append(dict(time=1.5,player='probe',type='packet',packet='VEHICLE_MOVE'))
        self.assertEqual('INCOMPLETE',summarize(c,p,e,'probe',340)['result'])
