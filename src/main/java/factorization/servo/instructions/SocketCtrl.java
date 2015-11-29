package factorization.servo.instructions;

import java.io.IOException;

import factorization.servo.stepper.StepperEngine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
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
    static final byte MODE_PULSE = 0, MODE_POWER = 1, MODE_UNPOWER = 2;
    byte mode = MODE_PULSE;

    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        mode = data.as(Share.VISIBLE, "mode").putByte(mode);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return Core.registry.empty_socket_item;
    }

    @Override
    public void stepperHit(StepperEngine engine) {
        if (mode == MODE_POWER) {
            engine.fzdsGrab();
        } else if (mode == MODE_UNPOWER) {
            engine.fzdsRelease();
        } else {
            if (engine.grabbed()) {
                engine.fzdsRelease();
            } else {
                engine.fzdsGrab();
            }
        }
    }

    @Override
    public void motorHit(ServoMotor motor) {
        if (mode == MODE_POWER) {
            motor.isSocketActive = true;
        } else if (mode == MODE_UNPOWER) {
            motor.isSocketActive = false;
        } else {
            motor.isSocketPulsed = true;
        }
    }

    @Override
    public IIcon getIcon(ForgeDirection side) {
        if (mode == 1) {
            return BlockIcons.servo$socket_on;
        } else if (mode == 2) {
            return BlockIcons.servo$socket_off;
        }
        return BlockIcons.servo$socket_pulse;
    }
    
    @Override
    public String getInfo() {
        if (mode == MODE_POWER) {
            return "Socket Powered";
        } else if (mode == MODE_UNPOWER) {
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
