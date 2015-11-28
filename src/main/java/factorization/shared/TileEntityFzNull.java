package factorization.shared;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntityFzNull extends TileEntity {
    static final String mapName = "fz.null";

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        // Hack for pistronics: load the TE specified by that tag at this position.
        if (worldObj == null) {
            super.readFromNBT(tag);
            return;
        }
        if (mapName.equals(tag.getString("id"))) return; // Avoid recursion death
        {
            int origX = tag.getInteger("x");
            int origY = tag.getInteger("y");
            int origZ = tag.getInteger("z");
            Core.logSevere("fz.null TileEntity reading a " + tag.getString("id") + " TileEntity at "
                    + xCoord + "," + yCoord + "," + zCoord + "; saved position's at "
                    + origX + "," + origY + "," + origZ);
        }
        tag.setInteger("x", this.xCoord);
        tag.setInteger("y", this.yCoord);
        tag.setInteger("z", this.zCoord);
        TileEntity replacement = TileEntity.createAndLoadEntity(tag);
        if (replacement == null) return; // Nothing can be done here.
        invalidate();
        replacement.validate();
        worldObj.setTileEntity(xCoord, yCoord, zCoord, replacement);
    }
}
