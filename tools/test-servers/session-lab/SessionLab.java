package viaforge.lab;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Isolated fixture: sends actual Bukkit resource requests and observes their statuses. */
public final class SessionLab extends JavaPlugin implements Listener {
    public void onEnable() { Bukkit.getPluginManager().registerEvents(this,this); }
    @EventHandler public void status(PlayerResourcePackStatusEvent event) {
        String id="legacy";try{id=String.valueOf(event.getClass().getMethod("getID").invoke(event));}catch(Exception ignored){}
        getLogger().info("PACK_STATUS "+event.getPlayer().getName()+" "+id+" "+event.getStatus());
    }
    public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(sender instanceof Player)return false;
        if(args.length<2)return false;
        Player player=Bukkit.getPlayerExact(args[1]);if(player==null)throw new IllegalArgumentException("Missing player");
        try {
            if(args[0].equals("pop")) {
                if(args.length==2)Player.class.getMethod("removeResourcePacks").invoke(player);
                else Player.class.getMethod("removeResourcePack",java.util.UUID.class).invoke(player,java.util.UUID.fromString(args[2]));
                getLogger().info("PACK_POP "+player.getName()+" "+(args.length==2?"all":args[2]));return true;
            }
            if(args[0].equals("push")) {
                byte[] hash=new byte[20];for(int i=0;i<20;i++)hash[i]=(byte)Integer.parseInt(args[4].substring(i*2,i*2+2),16);
                Player.class.getMethod("addResourcePack",java.util.UUID.class,String.class,byte[].class,String.class,boolean.class)
                    .invoke(player,java.util.UUID.fromString(args[2]),args[3],hash,"Local session fixture",false);
                getLogger().info("PACK_PUSH "+player.getName()+" "+args[2]+" "+args[4]);return true;
            }
        }catch(Exception failure){throw new IllegalStateException(failure);}
        if(args.length!=4||!args[0].equals("pack"))return false;
        byte[] hash=new byte[20];for(int i=0;i<20;i++)hash[i]=(byte)Integer.parseInt(args[3].substring(i*2,i*2+2),16);
        player.setResourcePack(args[2],hash);
        getLogger().info("PACK_REQUEST "+player.getName()+" "+args[3]);return true;
    }
}
