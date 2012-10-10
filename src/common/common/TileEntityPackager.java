package factorization.common;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.src.ItemStack;

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
    void doLogic() {
        int input_count = (input == null) ? 0 : input.stackSize;
        boolean can_add = output == null
                || output.stackSize < output.getMaxStackSize();
        if (outputBuffer == null && can_add && input != null
                && input.stackSize > 0) {
            ItemStack toCraft = null;
            int to_remove = 0;

            ItemCraft ic = Core.registry.item_craft;
            ItemStack craftInput = ItemStack.copyItemStack(input);
            if (input.stackSize >= 9) {
                toCraft = new ItemStack(ic);
                for (int i = 0; i < 9; i++) {
                    ic.addItem(toCraft, i, craftInput);
                }
                ic.craftAt(toCraft, true, this);
                if (!ic.isValidCraft(toCraft)) {
                    toCraft = null;
                }
                else {
                    to_remove = 9;
                }
            }
            if (input.stackSize >= 4 && toCraft == null) {
                toCraft = new ItemStack(ic);
                ic.addItem(toCraft, 0, craftInput);
                ic.addItem(toCraft, 1, craftInput);
                ic.addItem(toCraft, 3, craftInput);
                ic.addItem(toCraft, 4, craftInput);
                ic.craftAt(toCraft, true, this);
                if (!ic.isValidCraft(toCraft)) {
                    toCraft = null;
                }
                else {
                    to_remove = 4;
                }
            }

            if (toCraft == null) {
                return;
            }

            ArrayList<ItemStack> fakeResult = Core.registry.item_craft
                    .craftAt(toCraft, true, this);

            if (canMerge(fakeResult)) {
                //really craft
                ArrayList<ItemStack> craftResult = Core.registry.item_craft
                        .craftAt(toCraft, false, this);
                outputBuffer = craftResult;
                needLogic();
                drawActive(3);
                pulse();
                input.stackSize -= to_remove;
            }
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
                if (output.isItemEqual(here)) {
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
    }

}
