package factorization.docs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.item.ItemStack;

public class ItemListViewer implements IDocGenerator {
    @Override
    public void process(Typesetter sb, String arg) {
        HashMap<String, ItemStack> found = new HashMap();
        ArrayList<String> toSort = new ArrayList();
        for (Entry<String, ItemStack> pair : DocumentationModule.getNameItemCache().entrySet()) {
            ItemStack is = pair.getValue();
            String name = is.getDisplayName();
            found.put(name, is);
            toSort.add(name);
        }
        Collections.sort(toSort);
        
        for (String name : toSort) {
            ItemStack is = found.get(name);
            if (is == null) continue;
            sb.emitWord(new ItemWord(is, null));
            sb.append(" ");
            sb.emit(name, null);
            sb.append("\n\n");
        }
    }

}
