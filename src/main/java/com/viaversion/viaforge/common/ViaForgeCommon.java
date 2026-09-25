/*
 * This file is part of ViaForge - https://github.com/ViaVersion/ViaForge
 * Copyright (C) 2021-2026 Florian Reuth <git@florianreuth.de> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.viaversion.viaforge.common;

import com.viaversion.viabackwards.ViaBackwardsPlatformImpl;
import com.viaversion.viarewind.ViaRewindPlatformImpl;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.commands.ViaCommandHandler;
import com.viaversion.viaversion.connection.ConnectionDetails;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.platform.NoopInjector;
import com.viaversion.viaversion.platform.ViaDecodeHandler;
import com.viaversion.viaversion.platform.ViaEncodeHandler;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import com.viaversion.viaforge.common.extended.ExtendedNetworkManager;
import com.viaversion.viaforge.blocks.ClientBlocks;
import com.viaversion.viaforge.compatibility.ServerSession;
import com.viaversion.viaforge.common.platform.ViaForgeConfig;
import com.viaversion.viaforge.common.platform.ViaForgePlatform;
import com.viaversion.viaforge.common.protocoltranslator.platform.ViaForgePlatformLoader;
import com.viaversion.viaforge.common.protocoltranslator.platform.ViaForgeViaVersionPlatform;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.util.AttributeKey;
import java.io.File;
import net.raphimc.vialegacy.ViaLegacyPlatformImpl;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.netty.PreNettyLengthPrepender;
import net.raphimc.vialegacy.netty.PreNettyLengthRemover;

/**
 * Initializes protocol translation, injects ViaVersion into the Netty pipeline
 * and manages the target server version.
 */
public class ViaForgeCommon {

    public static final AttributeKey<UserConnection> VF_VIA_USER = AttributeKey.valueOf("viaforge_via_user");
    public static final AttributeKey<ExtendedNetworkManager> VF_NETWORK_MANAGER = AttributeKey.valueOf("viaforge_network_manager");

    private static ViaForgeCommon manager;

    private final ViaForgePlatform platform;
    private volatile ProtocolVersion targetVersion;

    private ViaForgeConfig config;

    public ViaForgeCommon(ViaForgePlatform platform) {
        this.platform = platform;
    }

    /**
     * Initializes the manager.
     *
     * @param platform the platform fields
     */
    public static void init(final ViaForgePlatform platform) {
        final ProtocolVersion version = ProtocolVersion.getProtocol(platform.getGameVersion()); // ViaForge will only load on post-netty versions
        if (version == ProtocolVersion.unknown) {
            throw new IllegalArgumentException("Unknown version " + platform.getGameVersion());
        }

        manager = new ViaForgeCommon(platform);

        final File mainFolder = new File(platform.getLeadingDirectory(), "ViaForge");

        ViaManagerImpl.initAndLoad(
            new ViaForgeViaVersionPlatform(mainFolder),
            new NoopInjector(),
            new ViaCommandHandler(false),
            new ViaForgePlatformLoader(platform),
            () -> {
                new ViaBackwardsPlatformImpl();
                new ViaRewindPlatformImpl();
                new ViaLegacyPlatformImpl();
                new com.viaversion.viaaprilfools.ViaAprilFoolsPlatformImpl();
            }
        );
        manager.config = new ViaForgeConfig(new File(mainFolder, "viaforge.yml"), Via.getPlatform().getLogger());

        final ProtocolVersion configVersion = ProtocolVersion.getClosest(manager.config.getClientSideVersion());
        if (configVersion != null) {
            manager.setTargetVersion(configVersion);
        } else {
            manager.setTargetVersion(version);
        }
    }

    /**
     * Injects the ViaVersion pipeline into the netty pipeline.
     *
     * @param channel the channel to inject the pipeline into
     */
    public void inject(final Channel channel, final ExtendedNetworkManager networkManager) {
        final UserConnection user = new UserConnectionImpl(channel, true);
        new ProtocolPipelineImpl(user).add(getPlatform().getCustomProtocol());

        channel.attr(VF_VIA_USER).set(user);
        channel.attr(VF_NETWORK_MANAGER).set(networkManager);

        final ChannelPipeline pipeline = channel.pipeline();

        // ViaVersion
        com.viaversion.viaforge.common.compatibility.CompatibilityProfile target =
                com.viaversion.viaforge.common.compatibility.CompatibilityRegistry.DEFAULT.resolve(networkManager.viaForge$getTrackedVersion().getVersion());
        user.put(target);
        ViaDecodeHandler decoder = !target.extended() ? new ViaDecodeHandler(user)
                : new com.viaversion.viaforge.common.compatibility.CompatibilityDecodeHandler(user,target.adapter().create(target,ClientBlocks::localState),
                        () -> ServerSession.join(channel,target), () -> ServerSession.leave(channel),
                        () -> target.rules().enabled(com.viaversion.viaforge.common.compatibility.ClientRule.CONCURRENT_CLIENT_TASKS)
                                && user.getProtocolInfo().getServerState()==com.viaversion.viaversion.api.protocol.packet.State.PLAY
                                && com.viaversion.viaforge.compatibility.ClientPacketTasks.beginPacket(target.rules().enabled(com.viaversion.viaforge.common.compatibility.ClientRule.DEDICATED_PACKET_QUEUE)),
                        com.viaversion.viaforge.compatibility.ClientPacketTasks::endPacket);
        pipeline.addBefore(platform.getDecodeHandlerName(), ViaDecodeHandler.NAME, decoder);
        pipeline.addBefore("encoder", ViaEncodeHandler.NAME, new ViaEncodeHandler(user));

        if (networkManager.viaForge$getTrackedVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
            // ViaLegacy
            pipeline.addBefore("splitter", PreNettyLengthPrepender.NAME, new PreNettyLengthPrepender(user));
            pipeline.addBefore("prepender", PreNettyLengthRemover.NAME, new PreNettyLengthRemover(user));
        }

    }

    public void sendConnectionDetails(final Channel channel) {
        ConnectionDetails.sendConnectionDetails(channel.attr(VF_VIA_USER).get(), ConnectionDetails.MOD_CHANNEL);
    }

    public ProtocolVersion getNativeVersion() {
        return ProtocolVersion.getProtocol(platform.getGameVersion());
    }

    public ProtocolVersion getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(final ProtocolVersion targetVersion) {
        if (targetVersion == null) {
            throw new IllegalArgumentException("Target version cannot be null");
        }
        this.targetVersion = targetVersion;
        config.setClientSideVersion(targetVersion.getName());
    }

    public ViaForgePlatform getPlatform() {
        return platform;
    }

    public ViaForgeConfig getConfig() {
        return config;
    }

    public static ViaForgeCommon getManager() {
        return manager;
    }

}
