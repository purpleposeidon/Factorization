package factorization.common.servo;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.datahelpers.DataHelper;

public class Controller extends ServoComponent {
    void doUpdate(ServoMotor motor) {
        
    }

    @Override
    public String getName() {
        return "servo.controller.default";
    }

    @Override
    public int getUniqueId() {
        return CONTROLLER_BASE;
    }

    @Override
    protected void putData(DataHelper data) throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    void onClick(EntityPlayer player, ForgeDirection side) {
        // TODO Auto-generated method stub
        
    }

    @Override
    void dropItems() {
        // TODO Auto-generated method stub
        
    }

    @Override
    @SideOnly(Side.CLIENT)
    void renderDynamic() {
        // TODO Auto-generated method stub
        
    }

    @Override
    @SideOnly(Side.CLIENT)
    void renderStatic() {
        // TODO Auto-generated method stub
        
    }
}
