package factorization.truth.gen;

import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OreDictionaryViewer implements IDocGenerator {

    @Override
    public void process(ITypesetter out, String arg) throws TruthError {
        Arrays.asList(OreDictionary.getOreNames());
        ArrayList<String> names = new ArrayList<String>();
        Collections.addAll(names, OreDictionary.getOreNames());
        Collections.sort(names);
        ArrayList<String> bountiful = new ArrayList<String>();
        ArrayList<String> empties = new ArrayList<String>();
        ArrayList<String> singles = new ArrayList<String>();
        for (String name : names) {
            List<ItemStack> ores = OreDictionary.getOres(name);
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
            if (prev) out.write("\\newpage");
            prev = true;
            for (String name : singles) {
                show(out, name);
            }
        }
        
        if (!empties.isEmpty()) {
            if (prev) out.write("\\newpage");
            prev = true;
            out.write("\\title{Empty Lists}");
            for (String name : empties) {
                out.write("\\nl");
                out.write(name);
            }
        }
    }
    
    void show(ITypesetter out, String name) throws TruthError {
        List<ItemStack> ores = OreDictionary.getOres(name);
        out.write("\\seg");
        out.write(String.format("\\nl %s: ", name));
        for (ItemStack is : ores) {
            out.write(is);
        }
        out.write("\\endseg");
    }
}
