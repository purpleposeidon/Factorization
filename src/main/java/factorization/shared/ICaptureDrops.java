package factorization.shared;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface ICaptureDrops {
    public boolean captureDrops(ArrayList<ItemStack> stacks);
}
