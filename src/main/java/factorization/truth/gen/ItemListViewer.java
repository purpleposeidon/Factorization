package factorization.truth.gen;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import factorization.shared.Core;
import factorization.truth.DocumentationModule;
import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import factorization.truth.word.ItemWord;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;

public class ItemListViewer implements IDocGenerator {
    @Override
    public void process(ITypesetter sb, String arg) throws TruthError {
        if (arg.equalsIgnoreCase("all")) {
            listAll(sb, null);
            return;
        }
        CreativeTabs found = null;
        for (CreativeTabs ct : CreativeTabs.creativeTabArray) {
            if (ct.getTabLabel().equalsIgnoreCase(arg)) {
                found = ct;
                break;
            }
        }
        if (found != null) {
            listAll(sb, found);
        } else {
            listTabs(sb);
        }
    }
    
    void listTabs(ITypesetter sb) throws TruthError {
        String ret = "";
        ret += "\\title{Item Categories}\n\n";
        ret += "\n\n\\link{cgi/items/all}{All Items}";
        for (CreativeTabs ct : CreativeTabs.creativeTabArray) {
            if (ct == CreativeTabs.tabAllSearch || ct == CreativeTabs.tabInventory) {
                continue;
            }
            String text = ct.getTabLabel();
            ret += "\\nl\\link{cgi/items/" + text + "}{" + Core.translateThis("itemGroup." + text) + "}";
        }
        sb.write(ret, null, "");
    }
    
    void listAll(ITypesetter out, CreativeTabs ct) throws TruthError {
        if (ct == null) {
            out.write("\\title{All Items}");
        } else {
            String title = ct.getTabLabel();
            title = Core.translateThis("itemGroup." + title);
            out.write("\\title{" + title + "}");
        }
        out.write("\n\n");
        int size = DocumentationModule.getNameItemCache().size();
        Multimap<String, ItemStack> found = HashMultimap.<String, ItemStack>create(size, 1);
        ArrayList<String> toSort = new ArrayList();
        for (Entry<String, ArrayList<ItemStack>> pair : DocumentationModule.getNameItemCache().entrySet()) {
            ArrayList<ItemStack> items = pair.getValue();
            for (ItemStack is : items) {
                if (ct != null && is.getItem().getCreativeTab() != ct) {
                    continue;
                }
                String name = is.getDisplayName();
                if (!found.containsKey(name)) {
                    toSort.add(name);
                }
                found.put(name, is);
            }
        }
        Collections.sort(toSort);
        
        for (String name : toSort) {
            for (ItemStack is : found.get(name)) {
                if (is == null) continue;
                out.write(new ItemWord(is));
                out.write(" ");
                out.write(name, null);
                out.write("\n\n");
            }
        }
    }

}
