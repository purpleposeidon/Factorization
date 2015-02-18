package factorization.compat;

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
        //TODO: SoundLogic'll give me item aspects?
        //TODO: Make sure we give a way to disable this!
        //TODO: Register TC scan handler for barrels; should give its contents rather than the barrel
        /*ThaumcraftApi.registerScanEventhandler(new IScanEventHandler() {
            
            @Override
            public ScanResult scanPhenomena(ItemStack stack, World world, EntityPlayer player) {
                return null;
            }
        });*/
    }
}
