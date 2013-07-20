package factorization.common;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;

public class TileEntityPackager extends TileEntityStamper {
    @Override
    public String getInvName() {
        return "Packager";
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.PACKAGER;
    }
    
    @Override
    public Icon getIcon(ForgeDirection dir) {
        return BlockIcons.packager.get(this, dir);
    }
    
    @Override
    protected List<ItemStack> tryCrafting() {
        ItemStack[] matrix = new ItemStack[9];
        List<ItemStack> testOutput = null;
        int to_remove = 0;
        ItemStack p = input.copy();
        p.stackSize = 1;
        if (input.stackSize >= 9) {
            for (int i = 0; i < 9; i++) {
                matrix[i] = p;
            }
            to_remove = 9;
            testOutput = FactorizationUtil.craft3x3(this, true, matrix);
        } else if (input.stackSize >= 4) {
            matrix[0] = p;
            matrix[1] = p;
            matrix[3] = p;
            matrix[4] = p;
            to_remove = 4;
            testOutput = FactorizationUtil.craft3x3(this, true, matrix);
        }
        
        if (testOutput == null || testOutput.isEmpty()) {
            return null;
        }

        if (!canMerge(testOutput)) {
            return null;
        }
        List<ItemStack> ret = FactorizationUtil.craft3x3(this, false, matrix);
        input.stackSize -= to_remove;
        input = FactorizationUtil.normalize(input);
        return ret;
    }
}
