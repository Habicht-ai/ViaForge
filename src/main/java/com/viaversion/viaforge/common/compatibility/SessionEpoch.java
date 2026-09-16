package com.viaversion.viaforge.common.compatibility;

/** Main-thread ownership for asynchronous resource work, including rejoin on the same channel. */
public final class SessionEpoch {
    public static final class Ticket {
        public final Object connection;
        public final CompatibilityProfile profile;
        private Ticket(Object connection,CompatibilityProfile profile){this.connection=connection;this.profile=profile;}
    }
    private volatile Ticket current;
    public Ticket begin(Object connection,CompatibilityProfile profile){return current=new Ticket(connection,profile);}
    public boolean current(Ticket ticket){return ticket!=null&&current==ticket;}
    public CompatibilityProfile profile(){return current==null?CompatibilityProfile.unsupported(47):current.profile;}
    public boolean leave(Object connection){if(current==null||current.connection!=connection)return false;current=null;return true;}
    public void clear(){current=null;}
}
