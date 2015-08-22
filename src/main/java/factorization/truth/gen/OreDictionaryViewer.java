package factorization.truth.gen;

import factorization.truth.AbstractTypesetter;
import factorization.truth.word.ItemWord;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class OreDictionaryViewer implements IDocGenerator {

    @Override
    public void process(AbstractTypesetter out, String arg) {
        Arrays.asList(OreDictionary.getOreNames());
        ArrayList<String> names = new ArrayList<String>();
        Collections.addAll(names, OreDictionary.getOreNames());
        Collections.sort(names);
        ArrayList<String> bountiful = new ArrayList<String>();
        ArrayList<String> empties = new ArrayList<String>();
        ArrayList<String> singles = new ArrayList<String>();
        for (String name : names) {
            ArrayList<ItemStack> ores = OreDictionary.getOres(name);
            if (ores == null || ores.isEmpty()) {
                empties.add(name);
            } else if (ores.size() == 1) {
                singles.add(name);
            } else {
                bountiful.add(name);
            }
        }
        
        boolean prev = false;
        if (!bountiful.isEmpty()) {
            prev = true;
            for (String name : bountiful) {
                show(out, name);
            }
        }
        
        if (!singles.isEmpty()) {
            if (prev) out.append("\\newpage");
            prev = true;
            for (String name : singles) {
                show(out, name);
            }
        }
        
        if (!empties.isEmpty()) {
            if (prev) out.append("\\newpage");
            prev = true;
            out.append("\\title{Empty Lists}");
            for (String name : empties) {
                out.append("\\nl");
                out.append(name);
            }
        }
    }
    
    void show(AbstractTypesetter out, String name) {
        ArrayList<ItemStack> ores = OreDictionary.getOres(name);
        out.append("\\seg");
        out.append(String.format("\\nl %s: ", name));
        for (ItemStack is : ores) {
            out.emitWord(new ItemWord(is));
        }
        out.append("\\endseg");
    }
}
