package factorization.scrap;

import factorization.util.DataUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Scanner;

@Help({"Sets the maximum stacksize of an item",
        "SetMaxSize minecraft:diamond 16"})
public class SetMaxSize implements IRevertible {
    final Item target;
    final int origSize, newSize;

    public SetMaxSize(Scanner in) {
        this.target = ScannerHelper.nextItem(in);
        this.newSize = in.nextInt();
        this.origSize = target.getItemStackLimit(new ItemStack(target));
    }


    @Override
    public void apply() {
        target.setMaxStackSize(newSize);
    }

    @Override
    public void revert() {
        target.setMaxStackSize(origSize);
    }

    @Override
    public String info() {
        return "ChangeMaxStackSize " + DataUtil.getName(target) + " " + newSize + " # original max stacksize " + origSize;
    }
}
