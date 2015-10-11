package factorization.truth.gen;

import cpw.mods.fml.relauncher.ReflectionHelper;
import factorization.truth.AbstractTypesetter;
import factorization.truth.api.IDocGenerator;
import factorization.truth.word.ItemWord;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class TreasureViewer implements IDocGenerator {

    @Override
    public void process(AbstractTypesetter out, String arg) {
        Map<String, ChestGenHooks> chestHooks = ReflectionHelper.<Map<String, ChestGenHooks>, ChestGenHooks>getPrivateValue(ChestGenHooks.class, null, "chestInfo");
        ArrayList<String> names = new ArrayList(chestHooks.keySet());
        Collections.sort(names);
        for (String chestName : names) {
            ChestGenHooks hook = chestHooks.get(chestName);
            ArrayList<WeightedRandomChestContent> content = ReflectionHelper.<ArrayList<WeightedRandomChestContent>, ChestGenHooks>getPrivateValue(ChestGenHooks.class, hook, "contents");
            if (content == null || content.isEmpty()) continue;
            content = new ArrayList(content);
            Collections.sort(content, new Comparator<WeightedRandomChestContent>() {
                @Override
                public int compare(WeightedRandomChestContent a, WeightedRandomChestContent b) {
                    return b.itemWeight - a.itemWeight;
                }
            });
            out.append("\\newpage \\title{Treasure: " + chestName + "}");
            boolean can_blob = false;
            for (WeightedRandomChestContent item : content) {
                if (!can_blob) out.append("\\p");
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
                    out.emitWord(new ItemWord(item.theItemId));
                } else {
                    if (can_blob) {
                        can_blob = false;
                        out.append("\\p");
                    }
                    out.emitWord(new ItemWord(item.theItemId));
                    out.append(descr);
                }
            }
        }
    }

}
