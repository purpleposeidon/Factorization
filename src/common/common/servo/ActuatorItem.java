package factorization.common.servo;

import java.io.IOException;
import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.Core;
import factorization.common.Core.TabType;
import factorization.common.ItemCraftingComponent;

public abstract class ActuatorItem extends ItemCraftingComponent {
    public ActuatorItem(int itemId, String name) {
        super(itemId, name);
        Core.tab(this, TabType.SERVOS);
    }

    public abstract boolean use(ItemStack is, EntityPlayer user, ServoMotor motor, MovingObjectPosition mop) throws IOException;
    
    static class TraceHelper extends EntityLiving {
        public TraceHelper() {
            super(null);
        }

        public ForgeDirection look = ForgeDirection.UP;
        @Override
        public int getMaxHealth() { return 0; }
        
        public Vec3 getLook() {
            return Vec3.createVectorHelper(look.offsetX, look.offsetY, look.offsetZ);
        }
    }
    
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }
        MovingObjectPosition mop = new MovingObjectPosition(x, y, z, side, Vec3.createVectorHelper(hitX, hitY, hitZ));
        try {
            use(stack, player, null, mop);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    public static boolean extractEnergy(ServoMotor user, int amount) {
        if (amount <= 0) {
            return true;
        }
        return true; //TODO: implement Actuator.extractEnergy
    }
    
    public abstract IDataSerializable getState();
    
    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        super.addInformation(is, player, infoList, verbose);
        try {
            addConfigurationInfo(is, infoList);
        } catch (IOException e) { }
    }
    
    public abstract void addConfigurationInfo(ItemStack is, List infoList) throws IOException;
}
