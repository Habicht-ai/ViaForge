package viaforge.lab;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.event.events.CompletePredictionEvent;
import ac.grim.grimac.api.event.events.FlagEvent;
import ac.grim.grimac.api.event.events.GrimPlayerSetbackEvent;
import ac.grim.grimac.api.event.events.GrimTeleportEvent;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.player.GrimPlayer;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.PacketEvents;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.event.PacketListenerAbstract;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.event.PacketListenerPriority;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerBoat;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

/** Local laboratory control. Never changes a check, threshold or cancellation result. */
public final class LabAC extends JavaPlugin implements Listener {
    private static final String PINNED_GRIM = "2.3.74-8eb5f28";
    private final Gson gson = new Gson();
    private final Map<UUID, PermissionAttachment> permissions = new HashMap<>();
    private final Map<UUID, Sample> samples = new ConcurrentHashMap<>();
    private GrimAbstractAPI api;
    private volatile boolean enabled;
    private volatile long generation;
    private boolean available;
    private PacketListenerAbstract packetTrace;
    private boolean traceBoats;

    private static final class Sample {
        final AtomicLong predictions = new AtomicLong(), flags = new AtomicLong(), setbacks = new AtomicLong();
        volatile Map<String, Object> actual = Collections.emptyMap();
        volatile long observedGeneration = -1;
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        enabled = getConfig().getBoolean("enabled", true);
        api = GrimAPI.INSTANCE.getExternalAPI();
        // The only implementation-specific access is READ-ONLY disableGrim/protocol diagnostics.
        // A different binary must be reviewed before this bridge can report its real state.
        if (!PINNED_GRIM.equals(api.getGrimVersion())) {
            getLogger().severe("Unsupported Grim build: " + api.getGrimVersion());
            writeStatus();
            return;
        }
        available = true;
        traceBoats = getConfig().getBoolean("trace-boats", false);
        if (getConfig().getBoolean("trace-packets", false)) {
            Map<UUID, AtomicLong> counts = new ConcurrentHashMap<>();
            packetTrace = new PacketListenerAbstract(PacketListenerPriority.MONITOR) {
                @Override public void onPacketReceive(PacketReceiveEvent event) {
                    if (event.getUser().getUUID() == null) return;
                    String packet = event.getPacketType().toString();
                    if (!Set.of("PLAYER_FLYING","PLAYER_POSITION","PLAYER_ROTATION","PLAYER_POSITION_AND_ROTATION",
                        "PONG","CLIENT_TICK_END","PLAYER_INPUT","STEER_VEHICLE","STEER_BOAT","VEHICLE_MOVE","ENTITY_ACTION","TELEPORT_CONFIRM").contains(packet)) return;
                    if (counts.computeIfAbsent(event.getUser().getUUID(),id -> new AtomicLong()).incrementAndGet() > (traceBoats ? 30000 : 1200)) return;
                    Map<String,Object> record=new LinkedHashMap<>(Map.of("player",event.getUser().getName(),"packet",packet,"cancelled",event.isCancelled()));
                    if(traceBoats) {
                        if(WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                            WrapperPlayClientPlayerFlying move=new WrapperPlayClientPlayerFlying(event);
                            record.put("position_changed",move.hasPositionChanged());record.put("rotation_changed",move.hasRotationChanged());
                            record.put("location",move.getLocation());record.put("ground",move.isOnGround());record.put("horizontal_collision",move.isHorizontalCollision());
                        } else if(packet.equals("ENTITY_ACTION")) {
                            record.put("action",new WrapperPlayClientEntityAction(event).getAction());
                        } else if(packet.equals("PLAYER_INPUT")) {
                            WrapperPlayClientPlayerInput input=new WrapperPlayClientPlayerInput(event);
                            record.put("forward",input.isForward());record.put("back",input.isBackward());record.put("left",input.isLeft());record.put("right",input.isRight());
                            record.put("jump",input.isJump());record.put("sneak",input.isShift());record.put("sprint",input.isSprint());
                        } else if(packet.equals("VEHICLE_MOVE")) {
                            WrapperPlayClientVehicleMove move=new WrapperPlayClientVehicleMove(event);
                            record.put("position",move.getPosition());record.put("yaw",move.getYaw());record.put("pitch",move.getPitch());record.put("ground",move.isOnGround());
                        } else if(packet.equals("STEER_BOAT")) {
                            WrapperPlayClientSteerBoat steer=new WrapperPlayClientSteerBoat(event);
                            record.put("left",steer.isLeftPaddleTurning());record.put("right",steer.isRightPaddleTurning());
                        } else if(packet.equals("STEER_VEHICLE")) {
                            WrapperPlayClientSteerVehicle steer=new WrapperPlayClientSteerVehicle(event);
                            record.put("forward",steer.getForward());record.put("sideways",steer.getSideways());record.put("jump",steer.isJump());record.put("dismount",steer.isUnmount());
                        }
                    }
                    logEvent("packet",record);
                }
            };
            PacketEvents.getAPI().getEventManager().registerListener(packetTrace);
        }
        GrimPlugin owner = api.getGrimPlugin(this);
        api.getEventBus().get(GrimTeleportEvent.class).onTeleport(owner,
            (user, teleport, timestamp) -> logEvent("teleport", Map.of("player",user.getName(),
                "teleport",teleport,"timestamp",timestamp)));
        api.getEventBus().get(GrimPlayerSetbackEvent.class).onPlayerSetback(owner,
            (user, teleport, x, y, z, timestamp) -> {
                samples.computeIfAbsent(user.getUniqueId(), id -> new Sample()).setbacks.incrementAndGet();
                logEvent("setback", Map.of("player", user.getName(), "teleport", teleport,
                    "x", x, "y", y, "z", z, "timestamp", timestamp));
            });
        api.getEventBus().get(CompletePredictionEvent.class).onCompletePrediction(owner,
            (user, check, offset, cancelled) -> {
                Sample s = samples.computeIfAbsent(user.getUniqueId(), id -> new Sample());
                long number = s.predictions.incrementAndGet();
                if (number <= 8 || offset > .001 || traceBoats && number <= 10000) {
                    GrimPlayer p = (GrimPlayer) user;
                    Map<String,Object> record=new LinkedHashMap<>(Map.of("player",user.getName(),"number",number,"offset",offset,
                        "x",p.x,"y",p.y,"z",p.z,"last_x",p.lastX,"last_y",p.lastY,"last_z",p.lastZ,
                        "transaction",p.lastTransactionReceived.get()));
                    if(traceBoats) {
                        record.put("predicted",p.predictedVelocity.vector);record.put("actual",p.actualMovement);
                        record.put("velocity",p.clientVelocity);record.put("vehicle",p.inVehicle());
                        record.put("water_level",p.vehicleData.waterLevel);record.put("vehicle_status",p.vehicleData.status);
                        record.put("old_status",p.vehicleData.oldStatus);record.put("land_friction",p.vehicleData.landFriction);
                        record.put("input_forward",p.vehicleData.vehicleForward);record.put("input_sideways",p.vehicleData.vehicleHorizontal);
                        record.put("last_yd",p.vehicleData.lastYd);record.put("box",p.boundingBox);
                        record.put("sprinting",p.isSprinting);record.put("previous_sprinting",p.lastSprinting);
                        record.put("sneaking",p.isSneaking);record.put("previous_sneaking",p.wasSneaking);record.put("slow_movement",p.isSlowMovement);
                        record.put("pose",p.pose);record.put("sneak_speed",p.sneakingSpeedMultiplier);
                    }
                    logEvent("prediction",record);
                }
                return cancelled;
            });
        api.getEventBus().get(FlagEvent.class).onFlagSupplier(owner,
            (user, check, verbose, cancelled) -> {
                samples.computeIfAbsent(user.getUniqueId(), id -> new Sample()).flags.incrementAndGet();
                logEvent("flag", Map.of("player", user.getName(), "uuid", user.getUniqueId().toString(),
                    "client", user.getVersionName(), "check", check.getCheckName(),
                    "verbose", verbose.get(), "cancelled", cancelled));
                return cancelled;
            });
        Bukkit.getPluginManager().registerEvents(this, this);
        for (Player player : Bukkit.getOnlinePlayers()) attach(player);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) observe(player);
            writeStatus();
        }, 1L, 20L);
        logEvent("startup", Map.of("enabled", enabled, "grim", api.getGrimVersion()));
    }

    private void attach(Player player) {
        PermissionAttachment attachment = permissions.computeIfAbsent(player.getUniqueId(), id -> player.addAttachment(this));
        attachment.setPermission("grim.verbose", true);
        if (enabled) attachment.unsetPermission("grim.disabled");
        else attachment.setPermission("grim.disabled", true);
        samples.computeIfAbsent(player.getUniqueId(), id -> new Sample());
        GrimUser user = api.getGrimUser(player.getUniqueId());
        if (user != null) user.updatePermissions();
    }

    private void observe(Player player) {
        if (!available) return;
        if (!permissions.containsKey(player.getUniqueId())) attach(player);
        GrimUser user = api.getGrimUser(player.getUniqueId());
        if (user == null || ((GrimPlayer)user).platformPlayer == null) return; // early join: retry next observation, never a false ON
        api.getAlertManager().setVerboseEnabled(user, true, true);
        user.updatePermissions();
        Sample sample = samples.get(player.getUniqueId());
        long revision = generation;
        String mode = player.getGameMode().name();
        boolean op = player.isOp();
        user.runSafely(() -> {
            GrimPlayer grim = (GrimPlayer) user;
            Map<String, Object> actual = new LinkedHashMap<>();
            actual.put("name", user.getName());
            actual.put("uuid", user.getUniqueId().toString());
            actual.put("client", user.getVersionName());
            actual.put("protocol", grim.getClientVersion().getProtocolVersion());
            actual.put("brand", user.getBrand());
            actual.put("gamemode", mode);
            actual.put("op", op);
            actual.put("disabled", grim.disableGrim);
            actual.put("verbose", api.getAlertManager().hasVerboseEnabled(user));
            actual.put("exempt_permission", user.hasPermission("grim.exempt"));
            actual.put("nosetback_permission", user.hasPermission("grim.nosetback"));
            actual.put("nomodifypacket_permission", user.hasPermission("grim.nomodifypacket"));
            actual.put("predictions", sample.predictions.get());
            actual.put("flags", sample.flags.get());
            actual.put("setbacks", sample.setbacks.get());
            actual.put("observed_at", System.currentTimeMillis());
            sample.actual = actual;
            sample.observedGeneration = revision;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST) public void login(PlayerLoginEvent event) {
        if (available && event.getResult() == PlayerLoginEvent.Result.ALLOWED) attach(event.getPlayer());
    }
    @EventHandler public void join(PlayerJoinEvent event) {
        attach(event.getPlayer());
        Bukkit.getScheduler().runTask(this, () -> observe(event.getPlayer()));
        logEvent("join", Map.of("player", event.getPlayer().getName(), "enabled", enabled));
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Sample sample = samples.remove(player.getUniqueId());
        if (sample != null) logEvent("quit", sample.actual);
        PermissionAttachment attachment = permissions.remove(player.getUniqueId());
        if (attachment != null) player.removeAttachment(attachment);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "status";
        if (!Set.of("on", "off", "status").contains(action) || args.length > 1) {
            sender.sendMessage("/labac on | off | status");
            return true;
        }
        if (!sender.hasPermission(action.equals("status") ? "labac.status" : "labac.control")) {
            sender.sendMessage("Missing labac permission");
            return true;
        }
        if (!available) { sender.sendMessage("LabAC UNAVAILABLE: unsupported or inactive Grim"); return true; }
        if (!action.equals("status")) {
            boolean wanted = action.equals("on");
            // Persist successfully BEFORE acknowledging or altering live permissions.
            boolean before = getConfig().getBoolean("enabled", true);
            getConfig().set("enabled", wanted);
            try {
                Path file=getDataFolder().toPath().resolve("config.yml"), temp=file.resolveSibling("config.yml.tmp");
                Files.writeString(temp,getConfig().saveToString(),StandardCharsets.UTF_8);
                Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException ex) {
                getConfig().set("enabled", before);
                sender.sendMessage("LabAC state could not be saved: " + ex.getMessage());
                return true;
            }
            enabled = wanted;
            generation++;
            for (Player player : Bukkit.getOnlinePlayers()) { attach(player); observe(player); }
            logEvent("switch", Map.of("enabled", enabled, "sender", sender.getName(), "generation", generation));
        }
        Map<String, Object> state = status();
        sender.sendMessage("LabAC " + state.get("state") + " requested=" + (enabled ? "ON" : "OFF")
            + " Grim=" + api.getGrimVersion() + " players=" + gson.toJson(state.get("players")));
        writeStatus();
        return true;
    }

    private Map<String, Object> status() {
        List<Map<String, Object>> users = new ArrayList<>();
        boolean consistent = available && Bukkit.getPluginManager().isPluginEnabled("GrimAC") && api.hasStarted();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Sample s = samples.get(player.getUniqueId());
            Map<String, Object> actual = s == null ? Collections.emptyMap() : s.actual;
            boolean current = s != null && s.observedGeneration == generation
                && actual.containsKey("disabled") && System.currentTimeMillis() - ((Number)actual.get("observed_at")).longValue() < 5000;
            consistent &= current && Boolean.valueOf(!enabled).equals(actual.get("disabled"))
                && Boolean.TRUE.equals(actual.get("verbose")) && Boolean.FALSE.equals(actual.get("exempt_permission"))
                && Boolean.FALSE.equals(actual.get("nosetback_permission")) && Boolean.FALSE.equals(actual.get("nomodifypacket_permission"));
            users.add(actual.isEmpty() ? Map.of("name", player.getName(), "pending", true) : actual);
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("time", System.currentTimeMillis() / 1000.0);
        state.put("state", !available ? "unavailable" : !consistent ? "pending" : enabled ? "enabled" : "disabled");
        state.put("requested_enabled", enabled);
        state.put("grim", api == null ? null : api.getGrimVersion());
        state.put("server", Bukkit.getVersion());
        state.put("players", users);
        state.put("generation", generation);
        return state;
    }

    private void writeStatus() {
        try {
            Path file = getDataFolder().toPath().resolve("status.json"), temp = file.resolveSibling("status.json.tmp");
            Files.writeString(temp, gson.toJson(status()), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) { getLogger().warning("Cannot publish actual state: " + ex.getMessage()); }
    }
    private synchronized void logEvent(String type, Map<String, ?> fields) {
        Map<String, Object> record = new LinkedHashMap<>(fields);
        record.put("type", type); record.put("time", System.currentTimeMillis() / 1000.0);
        try {
            Files.writeString(getDataFolder().toPath().resolve("events.jsonl"), gson.toJson(record) + "\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) { getLogger().warning("Cannot record event: " + ex.getMessage()); }
    }
    @Override public void onDisable() {
        if (packetTrace != null) PacketEvents.getAPI().getEventManager().unregisterListener(packetTrace);
        for (Player player : Bukkit.getOnlinePlayers()) {
            PermissionAttachment attachment = permissions.remove(player.getUniqueId());
            if (attachment != null) player.removeAttachment(attachment);
            if (api != null) {
                GrimUser user = api.getGrimUser(player.getUniqueId());
                if (user != null) user.updatePermissions();
            }
        }
        available = false;
        writeStatus();
    }
}
