package factorization.common.servo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.ItemCraftingComponent;
import factorization.common.Core.TabType;

public class ItemServoMotor extends ItemCraftingComponent {

    public ItemServoMotor(int par1) {
        super(par1, "servo/servo");
        Core.tab(this, TabType.SERVOS);
        setMaxStackSize(16);
    }
    
    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World w, int x, int y, int z, int side, float vecX, float vecY, float vecZ) {
        return false;
    }
    
    @Override
    public boolean onItemUseFirst(ItemStack is, EntityPlayer player, World w, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Coord c = new Coord(w, x, y, z);
        if (c.getTE(TileEntityServoRail.class) == null) {
            return false;
        }
        if (w.isRemote) {
            return false;
        }
        ServoMotor motor = new ServoMotor(w);
        motor.posX = c.x;
        motor.posY = c.y;
        motor.posZ = c.z;
        //c.setAsEntityLocation(motor);
        //w.spawnEntityInWorld(motor);
        motor.spawnServoMotor();
        ForgeDirection face = ForgeDirection.getOrientation(FactorizationUtil.determineOrientation(player));
        if (motor.validDirection(face, true)) {
            motor.orientation = FzOrientation.fromDirection(face);
            FzOrientation perfect = motor.orientation.pointTopTo(ForgeDirection.getOrientation(side));
            if (perfect != FzOrientation.UNKNOWN) {
                motor.orientation = perfect;
            }
        }
        if (!player.capabilities.isCreativeMode) {
            is.stackSize--;
        }
        return true;
    }
}
