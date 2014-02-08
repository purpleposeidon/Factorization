package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.CpuBlocking;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.shared.Core;

public class SocketCtrl extends Instruction {
    byte mode = 0;

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        mode = data.as(Share.VISIBLE, "mode").putByte(mode);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return Core.registry.empty_socket_item;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        if (mode == 1) {
            motor.isSocketActive = true;
        } else if (mode == 2) {
            motor.isSocketActive = false;
        } else {
            motor.isSocketPulsed = true;
        }
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        if (mode == 1) {
            return BlockIcons.servo$socket_on;
        } else if (mode == 2) {
            return BlockIcons.servo$socket_off;
        }
        return BlockIcons.servo$socket_pulse;
    }
    
    @Override
    public String getInfo() {
        if (mode == 1) {
            return "Socket Powered";
        } else if (mode == 2) {
            return "Socket Unpowered";
        }
        return "Socket Pulse";
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (!playerHasProgrammer(player)) return false;
        mode++;
        if (mode > 2) {
            mode = 0;
        }
        return true;
    }

    @Override
    public String getName() {
        return "fz.instruction.socketCtrl";
    }
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_FOR_TICK;
    }
}
