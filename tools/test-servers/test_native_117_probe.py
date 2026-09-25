import unittest
from native_117_probe import mapping_data


class OfficialMappingTest(unittest.TestCase):
    def test_hooks_use_descriptors_not_only_obfuscated_names(self):
        data = mapping_data('''# Official mapping format
net.minecraft.client.Minecraft -> mc:
    net.minecraft.client.player.LocalPlayer player -> p
    100:200:void tick() -> q
    201:202:void tick(boolean) -> q
net.minecraft.client.multiplayer.ClientPacketListener -> listener:
    1:2:void send(net.minecraft.network.protocol.Packet) -> a
    3:4:void handlePing(net.minecraft.network.protocol.game.ClientboundPingPacket) -> a
    5:6:void handleSetEntityData(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket) -> a
net.minecraft.network.protocol.Packet -> packet:
net.minecraft.network.protocol.game.ClientboundPingPacket -> ping:
net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket -> metadata:
    8:9:int getId() -> a
    10:10:void sample(int[],boolean[][]) -> b
''')
        self.assertEqual({'mc|q()V': 'tick', 'listener|a(Lpacket;)V': 'send',
                          'listener|a(Lping;)V': 'handlePing',
                          'listener|a(Lmetadata;)V': 'handleSetEntityData'}, data['hooks'])
        self.assertEqual('p', data['fields']['mc|player'])
        self.assertEqual(['a'], data['methods']['metadata|getId|0'])

    def test_reflection_retains_distinct_runtime_overloads(self):
        data = mapping_data('''net.minecraft.client.Minecraft -> mc:
    1:1:void tick() -> q
net.minecraft.client.multiplayer.ClientPacketListener -> listener:
    2:2:void lookup(int) -> a
    3:3:void lookup(java.lang.String) -> b
    4:4:void lookup(int):2 -> a
''')
        self.assertEqual(['a', 'b'], data['methods']['listener|lookup|1'])
