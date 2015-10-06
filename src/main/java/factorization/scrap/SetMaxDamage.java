package factorization.scrap;

import factorization.util.DataUtil;
import net.minecraft.item.Item;

import java.util.Scanner;

@Help({"Sets the max damage of an item",
        "ChangeMaxDamage minecraft:wooden_pickaxe 200"})
public class SetMaxDamage implements IRevertible {
    final Item target;
    final int newDamage, origDamage;

    public SetMaxDamage(Scanner in) {
        this.target = ScannerHelper.nextItem(in);
        this.newDamage = in.nextInt();
        this.origDamage = target.getMaxDamage();
    }


    @Override
    public void apply() {
        target.setMaxDamage(newDamage);
    }

    @Override
    public void revert() {
        target.setMaxDamage(origDamage);
    }

    @Override
    public String info() {
        return "ChangeMaxDamage " + DataUtil.getName(target) + " " + newDamage + " # original max damage " + origDamage;
    }
}
