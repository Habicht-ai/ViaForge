package com.viaversion.viaforge.common.compatibility;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;

/** Internal event schemas. A newer wire adapter may normalize into these schemas;
 * the number identifies the payload layout, never the actual connection version.
 * Existing native consumers retain their legacy representation behind this boundary.
 */
public final class ClientEventFormat {
    private static final java.util.Map<Integer,ClientEventFormat> FORMATS=new java.util.HashMap<>();
    static { for(BlockVersionProfile layout:BlockVersionProfile.values())FORMATS.put(layout.protocol(),new ClientEventFormat(layout)); }
    private final BlockVersionProfile layout;
    private ClientEventFormat(BlockVersionProfile layout){this.layout=layout;}
    public int revision(){return layout.protocol();}
    public String soundRegistryVersion(){return layout.resourceVersion();}
    public static ClientEventFormat legacy(int revision){
        ClientEventFormat format=FORMATS.get(revision);
        if(format==null)throw new IllegalArgumentException("Unknown internal event schema "+revision);
        return format;
    }
}
