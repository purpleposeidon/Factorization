package factorization.docs.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import factorization.docs.AbstractTypesetter;
import factorization.docs.word.ItemWord;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class OreDictionaryViewer implements IDocGenerator {

    @Override
    public void process(AbstractTypesetter out, String arg) {
        Arrays.asList(OreDictionary.getOreNames());
        ArrayList<String> names = new ArrayList();
        for (String name : OreDictionary.getOreNames()) {
            names.add(name);
        }
        Collections.sort(names);
        ArrayList<String> bountiful = new ArrayList();
        ArrayList<String> empties = new ArrayList();
        ArrayList<String> singles = new ArrayList();
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
