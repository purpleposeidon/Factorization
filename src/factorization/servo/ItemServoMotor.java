package factorization.servo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.ItemCraftingComponent;
import factorization.shared.Core.TabType;

public class ItemServoMotor extends ItemCraftingComponent {

    public ItemServoMotor() {
        super("servo/servo");
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
        ForgeDirection face = ForgeDirection.getOrientation(FzUtil.determineOrientation(player));
        if (motor.motionHandler.validDirection(face, true)) {
            motor.motionHandler.orientation = FzOrientation.fromDirection(face);
            FzOrientation perfect = motor.motionHandler.orientation.pointTopTo(ForgeDirection.getOrientation(side));
            if (perfect != FzOrientation.UNKNOWN) {
                motor.motionHandler.orientation = perfect;
            }
        }
        if (!player.capabilities.isCreativeMode) {
            is.stackSize--;
        }
        return true;
    }
}
