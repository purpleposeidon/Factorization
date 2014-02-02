package factorization.sockets.fanturpeller;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import factorization.shared.FzUtil;

public class CursorEntity extends Entity { //NORELEASE: Unused, right?
    int w, x, y, z;
    
    public CursorEntity(World thisWorld, SocketFanturpeller_BLEH te) {
        super(thisWorld);
        w = FzUtil.getWorldDimension(te.worldObj);
        x = te.xCoord;
        y = te.yCoord;
        z = te.zCoord;
    }
    
    @Override
    protected void entityInit() {
        
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
        // TODO Auto-generated method stub
        
    }
    
}