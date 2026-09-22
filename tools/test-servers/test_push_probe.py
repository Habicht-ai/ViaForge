import unittest
from push_probe import summarize


class PushEvidenceTest(unittest.TestCase):
    def fixture(self):
        player=dict(name='probe',protocol=340,gamemode='SURVIVAL',disabled=False,op=False,exempt_permission=False,verbose=True,nosetback_permission=False,nomodifypacket_permission=False)
        state=dict(state='enabled',players=[player])
        case=dict(case='Mob contact right',start=1,end=2,before=state,after=state)
        poses=[dict(time_ms=1000+i*50,phase='END',player_x=.5-i*.02,player_y=64,player_z=.5,
                    player_alive=True,nearby=[dict(overlap=True)]) for i in range(20)]
        events=[dict(time=1.5,player='probe',type='prediction')]
        return case,poses,events

    def test_contact_with_live_predictions(self):
        c,p,e=self.fixture();self.assertEqual('PASS',summarize(c,p,e,'probe',340)['result'])

    def test_empty_chat_dead_or_missing_contact_is_not_pass(self):
        for change in ('no_prediction','dead','no_contact','no_motion'):
            c,p,e=self.fixture()
            if change=='no_prediction':e=[]
            for pose in p:
                if change=='dead':pose['player_alive']=False
                if change=='no_contact':pose['nearby']=[]
                if change=='no_motion':pose['player_x']=.5
            self.assertEqual('FAIL',summarize(c,p,e,'probe',340)['result'],change)

    def test_enabled_protocol_mode_and_no_bypass_are_required(self):
        for key,value in [('disabled',True),('op',True),('exempt_permission',True),('gamemode','CREATIVE'),('protocol',47)]:
            c,p,e=self.fixture();c['before']['players'][0][key]=value
            self.assertEqual('FAIL',summarize(c,p,e,'probe',340)['result'],key)

    def test_team_block_requires_overlap_without_motion_and_flags_fail(self):
        c,p,e=self.fixture();c['case']='Mob team never'
        for pose in p:pose['player_x']=.5
        self.assertEqual('PASS',summarize(c,p,e,'probe',340)['result'])
        e.append(dict(time=1.5,player='probe',type='flag'))
        self.assertEqual('FAIL',summarize(c,p,e,'probe',340)['result'])
