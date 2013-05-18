package factorization.common.servo.controllers;

import java.io.IOException;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.datahelpers.DataHelper;
import factorization.common.servo.Controller;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class DummyController extends Controller {

    @Override
    public void doUpdate(ServoMotor motor) { }

    @Override
    public String getName() {
        return "fz.controller.dummyDefault";
    }

    @Override
    protected void putData(DataHelper data) throws IOException { }

    @Override
    public void onClick(EntityPlayer player, ForgeDirection side) { }

    @Override
    public List<ItemStack> dropItems() {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic() { }
    
    @Override
    public void configure(ServoStack stack) { }

}
