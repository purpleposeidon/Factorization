package factorization.fzds;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.chunk.Chunk;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.common.Core;
import factorization.common.Core.NotifyStyle;

public class TransferLib {
    private final String[] Lore = new String[] {
            "At twilight's end, the shadow's crossed, a new world birthed, the elder lost.",
            "Yet on the morn we wake to find that mem'ry left so far behind. To deafened ears we ask, unseen,",
            "“Which is life and which the dream?”"
    };
    
    static void setRaw(Coord c, int id, int md) {
        //c.setIdMd(id, md);
        Chunk chunk = c.w.getChunkFromBlockCoords(c.x, c.z);
        chunk.setBlockIDWithMetadata(c.x & 15, c.y, c.z & 15, id, md);
    }
    
    private static TileEntity wiper = new TileEntity();
    
    public static TileEntity move(Coord src, Coord dest) {
        Core.notify(null, src, NotifyStyle.FORCE, "-");
        int id = src.getId();
        int md = src.getMd();
        TileEntity te = src.getTE();
        NBTTagCompound teData = null;
        if (te != null) {
            teData = new NBTTagCompound();
            te.writeToNBT(teData);
            wiper.validate();
            src.setTE(wiper);
        }
        src.setId(0);
        wiper.worldObj = null; //Don't hold onto a reference
        setRaw(dest, id, md);
        if (teData != null) {
            teData.setInteger("x", dest.x);
            teData.setInteger("y", dest.y);
            teData.setInteger("z", dest.z);
            TileEntity ret = TileEntity.createAndLoadEntity(teData);
            ret.validate();
            dest.setTE(ret);
            return ret;
        }
        return null;
    }
    
    
}
