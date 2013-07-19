package factorization.common;

import java.util.Iterator;
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
    void doLogic() {
        int input_count = (input == null) ? 0 : input.stackSize;
        boolean need_pulse = false;
        boolean can_add = output == null || output.stackSize < output.getMaxStackSize();
        if (outputBuffer == null && can_add && input != null && input.stackSize > 0) {
            ItemStack[] matrix = new ItemStack[9];
            List<ItemStack> testOutput = null;
            int to_remove = 0;
            if (input.stackSize >= 9) {
                for (int i = 0; i < 9; i++) {
                    matrix[i] = input;
                }
                to_remove = 9;
                testOutput = FactorizationUtil.craft3x3(this, true, matrix);
            } else if (input.stackSize >= 4) {
                matrix[0] = input;
                matrix[1] = input;
                matrix[3] = input;
                matrix[4] = input;
                to_remove = 4;
                testOutput = FactorizationUtil.craft3x3(this, true, matrix);
            }
            
            if (testOutput == null || testOutput.isEmpty()) {
                return;
            }

            if (!canMerge(testOutput)) {
                return;
            }
            //really craft
            outputBuffer.addAll(FactorizationUtil.craft3x3(this, false, matrix));
            needLogic();
            drawActive(3);
            input.stackSize -= to_remove;
            need_pulse = true;
        }

        if (input != null && input.stackSize <= 0) {
            input = null;
        }

        if (outputBuffer != null) {
            // put outputBuffer into output
            Iterator<ItemStack> it = outputBuffer.iterator();
            while (it.hasNext()) {
                ItemStack here = it.next();
                if (here == null) {
                    it.remove();
                    continue;
                }
                if (output == null) {
                    output = here;
                    it.remove();
                    needLogic();
                    continue;
                }
                if (FactorizationUtil.couldMerge(output, here)) {
                    needLogic();
                    int can_take = output.getMaxStackSize() - output.stackSize;
                    if (here.stackSize > can_take) {
                        output.stackSize += can_take;
                        here.stackSize -= can_take; // will be > 0, keep in list
                        break; // output's full
                    }
                    output.stackSize += here.stackSize;
                    it.remove();
                }
            }
        }

        if (outputBuffer != null && outputBuffer.size() == 0) {
            // It got emptied. Maybe we can fit something else in?
            outputBuffer = null;
            needLogic();
        }
        int new_input_count = (input == null) ? 0 : input.stackSize;
        if (input_count != new_input_count) {
            needLogic();
        }
        if (need_pulse) {
            pulse();
        }
    }

}
