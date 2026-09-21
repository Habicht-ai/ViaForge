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

package com.viaversion.viaforge.common.platform;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Carries a connection's immutable selection into the synchronous NetworkManager
 * factory. The factory copies it before Netty initializes the channel on another
 * thread. Neither other connections nor status replies can change this selection.
 */
public final class VersionTracker {

    private static final ThreadLocal<ProtocolVersion> CONNECTING = new ThreadLocal<>();

    public static ProtocolVersion resolve(ProtocolVersion serverOverride, ProtocolVersion global) {
        return serverOverride != null ? serverOverride : Objects.requireNonNull(global, "Global protocol");
    }

    public static ProtocolVersion currentOr(ProtocolVersion fallback) {
        return resolve(CONNECTING.get(), fallback);
    }

    public static <T> T connect(ProtocolVersion version, Supplier<T> factory) {
        Objects.requireNonNull(version, "Connection protocol");
        ProtocolVersion previous = CONNECTING.get();
        CONNECTING.set(version);
        try {
            return factory.get();
        } finally {
            if (previous == null) CONNECTING.remove();
            else CONNECTING.set(previous);
        }
    }

    private VersionTracker() { }
}
