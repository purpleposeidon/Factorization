package factorization.servo.iterator;

import factorization.api.Coord;
import factorization.flat.api.FlatCoord;
import factorization.servo.rail.FlatServoRail;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.ItemCraftingComponent;
import factorization.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class ItemServoMotor extends ItemCraftingComponent {

    public ItemServoMotor(String name) {
        super("servo/" + name);
        Core.tab(this, TabType.SERVOS);
        setMaxStackSize(16);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
        return false;
    }

    protected AbstractServoMachine makeMachine(World w) {
        return new ServoMotor(w);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World w, BlockPos pos, EnumFacing top, float hitX, float hitY, float hitZ) {
        if (w.isRemote) {
            return false;
        }
        Coord c = new Coord(w, pos);
        FlatCoord flat = new FlatCoord(c, top);
        FlatServoRail fsr = flat.get(FlatServoRail.class);
        if (fsr == null) {
            return false;
        }
        AbstractServoMachine motor = makeMachine(w);
        motor.posX = c.x;
        motor.posY = c.y;
        motor.posZ = c.z;
        //c.setAsEntityLocation(motor);
        //w.spawnEntityInWorld(motor);

        PlayerUtil.cheatDecr(player, stack);
        motor.spawnServoMotor(top);
        return true;
    }
}
