package com.viaversion.viaforge.common.compatibility;

/** Wire order from ServerboundResourcePackPacket.Action (1.20.3+). */
public enum ServerPackStatus {
    SUCCESSFULLY_LOADED, DECLINED, FAILED_DOWNLOAD, ACCEPTED,
    DOWNLOADED, INVALID_URL, FAILED_RELOAD, DISCARDED;

    public int wireId(boolean singlePack) {
        return singlePack && ordinal() > 3 ? FAILED_DOWNLOAD.ordinal() : ordinal();
    }
}
