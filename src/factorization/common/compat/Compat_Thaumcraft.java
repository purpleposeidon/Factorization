package factorization.common.compat;

/*
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.research.IScanEventHandler;
import thaumcraft.api.research.ScanResult;*/



public class Compat_Thaumcraft {
    //Reminder: This needs to be mentioned in CompactManager
    public Compat_Thaumcraft() {
        //NORELEASE: Register TC scan handler for barrels
        //NORELEASE: Is azanor cool with this being released if TC4 isn't?
        /*ThaumcraftApi.registerScanEventhandler(new IScanEventHandler() {
            
            @Override
            public ScanResult scanPhenomena(ItemStack stack, World world, EntityPlayer player) {
                System.out.println("NORELEASE: " + stack);
                return null;
            }
        });*/
    }
}
