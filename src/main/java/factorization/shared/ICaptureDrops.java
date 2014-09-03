package factorization.shared;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;

public interface ICaptureDrops {

    public boolean captureDrops(int x,int y, int z, ArrayList<ItemStack> stacks);
    
}
