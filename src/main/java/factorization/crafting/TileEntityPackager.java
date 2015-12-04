package factorization.crafting;

import java.util.Arrays;
import java.util.List;

import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.util.CraftUtil;

import factorization.util.ItemUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.EnumFacing;

public class TileEntityPackager extends TileEntityStamper {
    @Override
    public String getInventoryName() {
        return "Packager";
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.PACKAGER;
    }
    
    @Override
    public IIcon getIcon(EnumFacing dir) {
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
            testOutput = CraftUtil.craft3x3(this, true, true, matrix);
        } else {
            CraftUtil.craft_succeeded = false;
        }
        if (input.stackSize >= 4 && !CraftUtil.craft_succeeded) {
            Arrays.fill(matrix, null);
            matrix[0] = p;
            matrix[1] = p;
            matrix[3] = p;
            matrix[4] = p;
            to_remove = 4;
            testOutput = CraftUtil.craft3x3(this, true, true, matrix);
        }
        
        if (!CraftUtil.craft_succeeded) {
            return null;
        }
        
        if (testOutput == null || testOutput.isEmpty()) {
            return null;
        }

        if (!canMerge(testOutput)) {
            return null;
        }
        List<ItemStack> ret = CraftUtil.craft3x3(this, false, false, matrix);
        input.stackSize -= to_remove;
        input = ItemUtil.normalize(input);
        return ret;
    }
}
