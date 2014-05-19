package factorization.servo.instructions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.base.Joiner;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.Executioner;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;
import factorization.shared.Core;

public class InstructionGroup extends Instruction {
    ServoStack stuff = new ServoStack(new Executioner(null));
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data)
            throws IOException {
        stuff = data.as(Share.VISIBLE, "contents").put(stuff);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Items.slime_ball);
    }
    
    @Override
    protected void addRecipes() {
        super.addRecipes();
        Core.registry.shapelessRecipe(toItem(), toItem()); 
    }

    @Override
    public void motorHit(ServoMotor motor) {
        Iterator<Object> it = stuff.descendingIterator();
        ServoStack ss = motor.getInstructionsStack();
        while (it.hasNext()) {
            ss.push(it.next());
        }
    }
    
    @Override
    public boolean onClick(EntityPlayer player, ServoMotor motor) {
        if (stuff.getSize() > 0) return false;
        ServoStack ss = motor.getArgStack();
        if (ss.getSize() <= 0) return false;
        while (ss.getSize() > 0) {
            stuff.push(ss.pop());
        }
        return true;
    }

    @Override
    public IIcon getIcon(ForgeDirection side) {
        if (stuff.getSize() > 0) {
            return BlockIcons.servo$group_something;
        } else {
            return BlockIcons.servo$group_empty;
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.group";
    }
    
    @Override
    public void addInformation(List info) {
        if (stuff.getSize() <= 0) {
            info.add("Empty");
            return;
        }
        Iterator<Object> it = stuff.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            info.add(obj.toString());
        }
    }
    
    @Override
    public String getInfo() {
        ArrayList<String> bits = new ArrayList();
        addInformation(bits);
        return Joiner.on("\n").join(bits);
    }

}
