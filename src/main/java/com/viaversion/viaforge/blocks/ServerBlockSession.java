package com.viaversion.viaforge.blocks;

import com.viaversion.viaforge.common.blocks.BlockVersionProfile;
import com.viaversion.viaforge.common.compatibility.CompatibilityRegistry;
import com.viaversion.viaforge.compatibility.ServerSession;

/** Legacy fixture API. Runtime modules use the feature-based ServerSession. */
@Deprecated
public final class ServerBlockSession {
    public static void initialize(){ServerSession.initialize();}
    public static void join(Object connection,BlockVersionProfile profile){ServerSession.join(connection,CompatibilityRegistry.DEFAULT.resolve(profile.protocol()));}
    public static void leave(Object connection){ServerSession.leave(connection);}
    public static void unload(){ServerSession.unload();}
    public static String getLoadedResourceVersion(){return ServerSession.getLoadedResourceVersion();}
    public static boolean supportsProtocol(int revision){return ServerSession.contentSince(revision);}
    public static boolean supports(com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition block){return ServerSession.supports(block);}
    public static boolean supportsItem(com.viaversion.viaforge.common.blocks.LegacyItemDefinition item){return ServerSession.supportsItem(item);}
    public static boolean supportsItem(com.viaversion.viaforge.common.blocks.LegacyBlockCatalog.Definition block){return ServerSession.supportsItem(block);}
    private ServerBlockSession(){ }
}
