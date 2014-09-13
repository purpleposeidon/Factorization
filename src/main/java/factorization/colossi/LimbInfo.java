package factorization.colossi;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.util.Vec3;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.colossi.ColossusController.BodySide;
import factorization.colossi.ColossusController.LimbType;
import factorization.fzds.api.IDeltaChunk;

class LimbInfo {
    static UUID fake_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
    LimbType type = LimbType.UNKNOWN_LIMB_TYPE;
    BodySide side = BodySide.UNKNOWN_BODY_SIDE;
    byte parity = 0; // If you have a centipede, you kind of need to swap left & right every other foot
    int length; // How long the limb is
    UUID entityId = fake_uuid;
    IDeltaChunk ent = null;
    Vec3 originalBodyOffset; // Joint position, relative to the body's DSE.
    
    // The following transformations are applied in the order given here.
    // They aren't relevant to LimbType.BODY.
    // Angles are in radians.
    double extension = 0; // Translation along the limb's local Y axis
    double twist = 0; // Rotation on the local Y axis
    double swing = 0; // Rotation on the local Z axis. Swinging arms/legs.
    double sweep = 0; // Rotation on the global Y axis
    double flap = 0; // Rotation on the global X axis
    
    transient boolean act_violently = false;
    
    public LimbInfo() { }
    
    public LimbInfo(LimbType type, BodySide side, int length, IDeltaChunk ent) {
        this.type = type;
        this.side = side;
        this.length = length;
        this.ent = ent;
        this.entityId = ent.getUniqueID();
    }


    void putData(DataHelper data, int index) throws IOException {
        type = data.as(Share.PRIVATE, "limbType" + index).putEnum(type);
        side = data.as(Share.PRIVATE, "limbSide" + index).putEnum(side);
        parity = data.as(Share.PRIVATE, "limbParity" + index).putByte(parity);
        length = data.as(Share.PRIVATE, "limbLength" + index).putInt(length);
        entityId = data.as(Share.PRIVATE, "entityId" + index).putUUID(entityId);
        if (data.isReader()) {
            originalBodyOffset = Vec3.createVectorHelper(0, 0, 0);
        }
        originalBodyOffset.xCoord = data.as(Share.PRIVATE, "bodyX" + index).putDouble(originalBodyOffset.xCoord);
        originalBodyOffset.yCoord = data.as(Share.PRIVATE, "bodyY" + index).putDouble(originalBodyOffset.yCoord);
        originalBodyOffset.zCoord = data.as(Share.PRIVATE, "bodyZ" + index).putDouble(originalBodyOffset.zCoord);
        extension = data.as(Share.PRIVATE, "extension" + index).putDouble(extension);
        twist = data.as(Share.PRIVATE, "twist" + index).putDouble(twist);
        swing = data.as(Share.PRIVATE, "swing" + index).putDouble(swing);
        sweep = data.as(Share.PRIVATE, "sweep" + index).putDouble(sweep);
        flap = data.as(Share.PRIVATE, "flap" + index).putDouble(flap);
    }
    
    void setOffsetFromBody(IDeltaChunk body) {
        originalBodyOffset = Vec3.createVectorHelper(ent.posX - body.posX, ent.posY - body.posY, ent.posZ - body.posZ);
    }
    
    boolean limbSwingParity() {
        return side == BodySide.RIGHT ^ (parity % 1 == 0) ^ type == LimbType.ARM;
    }
}