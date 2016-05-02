package factorization.servo.instructions;

import com.google.common.base.Joiner;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.Executioner;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;
import factorization.shared.Core;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InstructionGroup extends Instruction {
    ServoStack stuff = new ServoStack(new Executioner(null));
    
    @Override
    public IDataSerializable putData(String prefix, DataHelper data)
            throws IOException {
        stuff = data.as(Share.VISIBLE, "contents").putIDS(stuff);
        return this;
    }

    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Items.slime_ball);
    }
    
    @Override
    protected void addRecipes() {
        super.addRecipes();
        Core.registry.vanillaShapelessRecipe(toItem(), toItem()); 
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
        ArrayList<String> bits = new ArrayList<String>();
        addInformation(bits);
        return Joiner.on("\n").join(bits);
    }

    static IFlatModel filled, empty;
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return stuff.getSize() == 0 ? empty : filled;
    }

    @Override
    protected void loadModels(IModelMaker maker) {
        filled = reg(maker, "instruction_group/filled");
        empty = reg(maker, "instruction_group/empty");
    }

}
