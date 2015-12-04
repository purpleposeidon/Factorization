package factorization.truth.gen;

import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class TreasureViewer implements IDocGenerator {

    @Override
    public void process(ITypesetter out, String arg) throws TruthError {
        Map<String, ChestGenHooks> chestHooks = ReflectionHelper.<Map<String, ChestGenHooks>, ChestGenHooks>getPrivateValue(ChestGenHooks.class, null, "chestInfo");
        ArrayList<String> names = new ArrayList<String>(chestHooks.keySet());
        Collections.sort(names);
        for (String chestName : names) {
            ChestGenHooks hook = chestHooks.get(chestName);
            ArrayList<WeightedRandomChestContent> content = ReflectionHelper.getPrivateValue(ChestGenHooks.class, hook, "contents");
            if (content == null || content.isEmpty()) continue;
            content = new ArrayList<WeightedRandomChestContent>(content);
            Collections.sort(content, new Comparator<WeightedRandomChestContent>() {
                @Override
                public int compare(WeightedRandomChestContent a, WeightedRandomChestContent b) {
                    return b.itemWeight - a.itemWeight;
                }
            });
            out.write("\\newpage \\title{Treasure: " + chestName + "}");
            boolean can_blob = false;
            for (WeightedRandomChestContent item : content) {
                if (!can_blob) out.write("\\p");
                String descr = null;
                if (item.theMinimumChanceToGenerateItem == item.theMaximumChanceToGenerateItem) {
                    if (item.theMinimumChanceToGenerateItem != 1) {
                        descr = " (" + item.theMinimumChanceToGenerateItem + ")";
                    }
                } else {
                    descr = " (" + item.theMinimumChanceToGenerateItem + " to " + item.theMaximumChanceToGenerateItem + ")";
                }
                if (descr == null) {
                    can_blob = true;
                    out.write(item.theItemId);
                } else {
                    if (can_blob) {
                        can_blob = false;
                        out.write("\\p");
                    }
                    out.write(item.theItemId);
                    out.write(descr);
                }
            }
        }
    }

}
