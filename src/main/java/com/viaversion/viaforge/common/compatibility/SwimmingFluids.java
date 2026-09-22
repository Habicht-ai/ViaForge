package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.*;
import java.util.*;
import java.util.function.IntUnaryOperator;

/** Detached fluid snapshots. Wire capture and game-thread application never share mutable sections. */
public final class SwimmingFluids {
    private final Map<Long,byte[][]> chunks=new HashMap<>();
    private static long key(int x,int z){return (long)x<<32|(z&0xffffffffL);}
    /** 0 dry, 1..16 water level+1, 17/18 upward/downward bubble column. */
    public static int descriptor(String state) {
        if(state==null)return 0;
        String name=state.split("\\[",2)[0].replace("minecraft:","");
        if(name.equals("bubble_column"))return state.contains("drag=true")?18:17;
        if(name.equals("water")) {
            int start=state.indexOf("level="); if(start<0)return 1;
            int end=state.indexOf(']',start),comma=state.indexOf(',',start);if(comma>=0&&comma<end)end=comma;
            return 1+Integer.parseInt(state.substring(start+6,end));
        }
        return state.contains("waterlogged=true")||name.equals("kelp")||name.equals("kelp_plant")||name.equals("seagrass")||name.equals("tall_seagrass")?1:0;
    }
    private static ByteBuf event(int kind) {
        ByteBuf out=Unpooled.buffer();Types.VAR_INT.writePrimitive(out,0x3f);Types.STRING.write(out,"VF|entity");
        out.writeShort(340).writeByte(35).writeByte(kind);return out;
    }
    public static ByteBuf chunk(Chunk chunk,IntUnaryOperator fluid) {
        ByteBuf out=event(0);out.writeInt(chunk.getX()).writeInt(chunk.getZ()).writeBoolean(chunk.isFullChunk()).writeShort(chunk.getBitmask());
        for(int s=0;s<16;s++)if((chunk.getBitmask()&(1<<s))!=0) {
            ChunkSection section=chunk.getSections()[s];DataPalette blocks=section==null?null:section.palette(PaletteType.BLOCKS);
            for(int i=0;i<4096;i++)out.writeByte(blocks==null?0:fluid.applyAsInt(blocks.idAt(i)));
        }
        return out;
    }
    public static ByteBuf block(BlockPosition pos,int descriptor) {
        ByteBuf out=event(1);out.writeInt(pos.x()).writeInt(pos.y()).writeInt(pos.z()).writeByte(descriptor);return out;
    }
    public static ByteBuf unload(int x,int z){return event(2).writeInt(x).writeInt(z);}
    public void clear(){chunks.clear();}
    public int get(int x,int y,int z) {
        if(y<0||y>=256)return -1;byte[][] column=chunks.get(key(x>>4,z>>4));
        if(column==null)return -1;byte[] section=column[y>>4];
        return section==null?0:section[(y&15)<<8|(z&15)<<4|(x&15)]&255;
    }
    public void accept(ByteBuf input) {
        int kind=input.readUnsignedByte(),x=input.readInt();
        if(kind==0) {
            int z=input.readInt();boolean full=input.readBoolean();int mask=input.readUnsignedShort();
            byte[][] column=full?new byte[16][]:chunks.computeIfAbsent(key(x,z),ignored->new byte[16][]);
            for(int s=0;s<16;s++)if((mask&(1<<s))!=0){
                byte[] section=new byte[4096];input.readBytes(section);boolean wet=false;
                for(byte fluid:section)if(fluid!=0){wet=true;break;}
                column[s]=wet?section:null;
            }
            chunks.put(key(x,z),column);
        }else if(kind==1) {
            int y=input.readInt(),z=input.readInt(),value=input.readUnsignedByte();if(y<0||y>=256)return;
            byte[][] column=chunks.computeIfAbsent(key(x>>4,z>>4),ignored->new byte[16][]);
            if(column[y>>4]==null){if(value==0)return;column[y>>4]=new byte[4096];}
            column[y>>4][(y&15)<<8|(z&15)<<4|(x&15)]=(byte)value;
        }else if(kind==2)chunks.remove(key(x,input.readInt()));
        else throw new IllegalArgumentException("Unknown fluid update "+kind);
    }
}
