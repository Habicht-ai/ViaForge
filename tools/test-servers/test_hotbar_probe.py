import unittest
from hotbar_probe import summarize


class HotbarEvidenceTest(unittest.TestCase):
    def sample(self):
        player=dict(name='probe',protocol=776,disabled=False,op=False,exempt_permission=False,
                    nosetback_permission=False,nomodifypacket_permission=False)
        state=dict(state='enabled',players=[player])
        case=dict(start=1,end=3,before=state,after=state,expected='dirt',expected_slot=0,
                  server_expected=True,server_slot_expected=True)
        ticks=[dict(time_ms=1000+i*50,phase='END',selected_slot=0,
                    hotbar=[dict(item='minecraft:dirt')]+[None]*8) for i in range(40)]
        return case,ticks,[]

    def test_both_sides_agree(self):
        c,p,e=self.sample();self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])

    def test_empty_hand_cannot_use_previous_block(self):
        c,p,e=self.sample();c.update(expected='air',expected_slot=1)
        for tick in p:tick['selected_slot']=1
        self.assertEqual('PASS',summarize(c,p,e,'probe',776)['result'])
        c['server_expected']=False
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])

    def test_inventory_or_selection_divergence_cannot_pass(self):
        for mode in ('client_item','client_slot','server_slot','server_block','no_ticks'):
            c,p,e=self.sample()
            if mode=='client_item':p[-1]['hotbar']=[None]*9
            if mode=='client_slot':p[-1]['selected_slot']=1
            if mode=='server_slot':c['server_slot_expected']=False
            if mode=='server_block':c['server_expected']=False
            if mode=='no_ticks':p=[]
            self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'],mode)

    def test_flags_setbacks_and_disabled_checks_cannot_pass(self):
        for mode in ('flag','setback','disabled','wrong_protocol','exempt_permission','nosetback_permission','nomodifypacket_permission'):
            c,p,e=self.sample()
            if mode in ('flag','setback'):e.append(dict(type=mode,time=2))
            elif mode=='wrong_protocol':c['after']['players'][0]['protocol']=47
            else:c['after']['players'][0][mode]=True
            self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'],mode)

    def test_one_registered_sound_with_actual_audio_source(self):
        for mode in ('valid','missing','duplicate','wrong_sound','unregistered','no_source','wrong_pitch'):
            c,p,e=self.sample();c.update(expected_sound='gravel',require_sound_source=True)
            sound=dict(time_ms=1500,phase='sound',sound='viaforge:mob_sound.4.block.gravel.place',x=.5,y=64.5,z=.5,volume=1,pitch=.8,registered=True)
            source=dict(sound,phase='sound_source')
            if mode=='wrong_sound':sound['sound']='minecraft:dig.gravel'
            if mode=='unregistered':sound['registered']=False
            if mode=='wrong_pitch':sound['pitch']=1
            if mode!='missing':p.append(sound)
            if mode=='duplicate':p.append(sound.copy())
            if mode!='no_source':p.append(source)
            self.assertEqual('PASS' if mode=='valid' else 'FAIL',summarize(c,p,e,'probe',776)['result'],mode)

    def test_empty_hand_does_not_emit_placement_sound(self):
        c,p,e=self.sample();c.update(expected='air',expected_slot=1,expected_sound=None)
        for tick in p:tick['selected_slot']=1
        p.append(dict(time_ms=1500,phase='sound',sound='viaforge:mob_sound.4.block.gravel.place',x=.5,y=64.5,z=.5))
        self.assertEqual('FAIL',summarize(c,p,e,'probe',776)['result'])
