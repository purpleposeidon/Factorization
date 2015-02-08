package factorization.servo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.ItemCraftingComponent;

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
        ForgeDirection top = ForgeDirection.getOrientation(side);
        
        ArrayList<FzOrientation> valid = new ArrayList();
        motor.motionHandler.beforeSpawn();
        
        ForgeDirection playerAngle = ForgeDirection.getOrientation(SpaceUtil.determineOrientation(player));
        
        for (ForgeDirection fd : ForgeDirection.VALID_DIRECTIONS) {
            if (top == fd || top.getOpposite() == fd) {
                continue;
            }
            if (motor.motionHandler.validDirection(fd, false)) {
                FzOrientation t = FzOrientation.fromDirection(fd).pointTopTo(top);
                if (t != FzOrientation.UNKNOWN) {
                    if (fd == playerAngle) {
                        valid.clear();
                        valid.add(t);
                        break;
                    }
                    valid.add(t);
                }
            }
        }
        final Vec3 vP = Vec3.createVectorHelper(hitX, hitY, hitZ).normalize();
        Collections.sort(valid, new Comparator<FzOrientation>() {
            @Override
            public int compare(FzOrientation a, FzOrientation b) {
                double dpA = vP.dotProduct(Vec3.createVectorHelper(a.facing.offsetX, a.facing.offsetY, a.facing.offsetZ));
                double dpB = vP.dotProduct(Vec3.createVectorHelper(b.facing.offsetX, b.facing.offsetY, b.facing.offsetZ));
                double theta_a = Math.acos(dpA);
                double theta_b = Math.acos(dpB);
                if (theta_a > theta_b) {
                    return 1;
                } else if (theta_a < theta_b) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        if (!valid.isEmpty()) {
            motor.motionHandler.orientation = valid.get(0);
        }
        motor.motionHandler.prevOrientation = motor.motionHandler.orientation;
        if (!player.capabilities.isCreativeMode) {
            is.stackSize--;
        }
        motor.spawnServoMotor();
        return true;
    }
}
