package factorization.net;

import factorization.shared.NORELEASE;
import io.netty.buffer.ByteBuf;

public enum StandardMessageType {
    @Deprecated // 0 refers to a custom value
    UnusedZeroDummy,
    PlaySound,
    DrawActive,
    TileFzType,
    DescriptionRequest,
    DataHelperEdit,
    RedrawOnClient,
    DataHelperEditOnEntity,
    OpenDataHelperGui,
    OpenDataHelperGuiOnEntity,
    TileEntityMessageOnEntity,
    entity_sync,
    playerCommand,

    // Some generic messages
    Description, DoAction, SetSpeed, SetAmount, SetHeat, ParticleInfo, SetWorking,



    // I don't have any where to put these ones
    UtilityGooState,
    ArtifactForgeName, ArtifactForgeError;

    public static final StandardMessageType[] VALUES = values();
    static {
        NORELEASE.fixme("TE-specific methods should be moved into the TEs");
    }

    private final byte id = (byte) ordinal();
    {
        if (id < 0) {
            throw new IllegalArgumentException("Too many message types!");
        }
    }

    private static StandardMessageType fromId(byte id) {
        if (id < 0 || id >= VALUES.length) {
            return null;
        }
        return VALUES[id];
    }
}
