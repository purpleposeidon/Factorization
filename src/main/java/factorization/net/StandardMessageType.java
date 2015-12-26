package factorization.net;

import factorization.shared.NORELEASE;
import io.netty.buffer.ByteBuf;

public enum StandardMessageType {
    factorizeCmdChannel,
    PlaySound,

    DrawActive, FactoryType, DescriptionRequest, DataHelperEdit, RedrawOnClient, DataHelperEditOnEntity(true), OpenDataHelperGui, OpenDataHelperGuiOnEntity(true),
    TileEntityMessageOnEntity(true),
    BarrelDescription, BarrelItem, BarrelCount, BarrelDoubleClickHack,
    BatteryLevel, LeydenjarLevel,
    MirrorDescription,
    TurbineWater, TurbineSpeed,
    HeaterHeat,
    LaceratorSpeed,
    MixerSpeed, FanturpellerSpeed,
    CrystallizerInfo,
    WireFace,
    SculptDescription, SculptNew, SculptMove, SculptRemove, SculptState,
    ExtensionInfo, RocketState,
    ServoRailDecor, ServoRailEditComment,
    CompressionCrafter, CompressionCrafterBeginCrafting, CompressionCrafterBounds,
    ScissorState,
    GeneratorParticles,
    BoilerHeat,
    ShaftGenState,
    MillVelocity,
    MisanthropicSpawn, MisanthropicCharge,

    // Messages to entities; (true) marks that they are entity messages.
    servo_brief(true), servo_item(true), servo_complete(true), servo_stopped(true),
    entity_sync(true),
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
