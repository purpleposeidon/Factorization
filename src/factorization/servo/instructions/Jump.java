package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.Executioner;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class Jump extends Instruction {
    byte mode = Executioner.JMP_NEXT_INSTRUCTION;
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        mode = data.as(Share.MUTABLE, "mode").putByte(mode);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Block.pistonBase);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        if (mode == Executioner.JMP_NEXT_INSTRUCTION) {
            Boolean b = motor.getArgStack().popType(Boolean.class);
            motor.executioner.stacks_changed = true;
            if (b == null) {
                motor.putError("Jump: Stack Underflow of Boolean");
                return;
            }
            if (b) {
                motor.executioner.jmp = mode;
                motor.penalizeSpeed();
            }
        } else {
            motor.executioner.jmp = mode;
        }
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        if (mode == Executioner.JMP_NEXT_INSTRUCTION) {
            return BlockIcons.servo$jmp_instruction;
        } else if (mode == Executioner.JMP_NEXT_TILE) {
            return BlockIcons.servo$jmp_tile;
        } else {
            return BlockIcons.error;
        }
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (!playerHasProgrammer(player)) {
            return super.onClick(player, block, side);
        }
        if (mode == Executioner.JMP_NEXT_INSTRUCTION) {
            mode = Executioner.JMP_NEXT_TILE;
        } else {
            mode = Executioner.JMP_NEXT_INSTRUCTION;
        }
        return true;
    }
    
    @Override
    public String getInfo() {
        if (mode == Executioner.JMP_NEXT_INSTRUCTION) {
            return "Jump next";
        } else if (mode == Executioner.JMP_NEXT_TILE) {
            return "Unconditional Skip";
        } else {
            return "?";
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.jmp";
    }

}
