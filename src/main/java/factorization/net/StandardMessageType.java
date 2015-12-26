package factorization.net;

import factorization.shared.NORELEASE;
import io.netty.buffer.ByteBuf;

public enum StandardMessageType {
    UnusedZeroDummy,
    PlaySound,
    DrawActive,
    FactoryType,
    DescriptionRequest,
    DataHelperEdit,
    RedrawOnClient,
    DataHelperEditOnEntity(true),
    OpenDataHelperGui,
    OpenDataHelperGuiOnEntity(true),
    TileEntityMessageOnEntity(true),
    entity_sync(true),
    factorizeCmdChannel,

    // Some generic messages
    Description, DoAction, SetSpeed, SetAmount, SetHeat, ParticleInfo, SetWorking,


    SculptNew, SculptMove, SculptRemove, SculptState,
    ServoRailDecor, ServoRailEditComment,
    CompressionCrafterBounds,

    // Messages to entities; (true) marks that they are entity messages.
    servo_brief(true), servo_item(true), servo_complete(true), servo_stopped(true),
    UtilityGooState(true),

    // Messages to/from the player
    ArtifactForgeName(false, true), ArtifactForgeError(false, true);

    public static final StandardMessageType[] VALUES = values();
    public final boolean isEntityMessage, isPlayerMessage;

    private final byte id;
    StandardMessageType() {
        this(false, false);
        NORELEASE.fixme("TE-specific methods should be moved into the TEs");
    }

    StandardMessageType(boolean isEntity, boolean isPlayer) {
        id = (byte) ordinal();
        if (id < 0) {
            throw new IllegalArgumentException("Too many message types!");
        }
        isEntityMessage = isEntity;
        isPlayerMessage = isPlayer;
    }

    StandardMessageType(boolean isEntity) {
        this(true, false);
    }

    private static StandardMessageType fromId(byte id) {
        if (id < 0 || id >= VALUES.length) {
            return null;
        }
        return VALUES[id];
    }

    public static StandardMessageType read(ByteBuf in) {
        byte b = in.readByte();
        return fromId(b);
    }

    public void write(ByteBuf out) {
        out.writeByte(id);
    }

}
