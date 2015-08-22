package factorization.docs.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;

import factorization.docs.AbstractTypesetter;
import factorization.docs.DocumentationModule;
import factorization.docs.word.ItemWord;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import factorization.shared.Core;

public class ItemListViewer implements IDocGenerator {
    @Override
    public void process(AbstractTypesetter sb, String arg) {
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
    
    void listTabs(AbstractTypesetter sb) {
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
        sb.process(ret, null, "");
    }
    
    void listAll(AbstractTypesetter sb, CreativeTabs ct) {
        if (ct == null) {
            sb.append("\\title{All Items}");
        } else {
            String title = ct.getTabLabel();
            title = Core.translateThis("itemGroup." + title);
            sb.append("\\title{" + title + "}");
        }
        sb.append("\n\n");
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
                sb.emitWord(new ItemWord(is));
                sb.append(" ");
                sb.emit(name, null);
                sb.append("\n\n");
            }
        }
    }

}
