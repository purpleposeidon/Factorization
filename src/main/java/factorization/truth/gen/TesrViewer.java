package factorization.truth.gen;

import factorization.truth.AbstractTypesetter;
import factorization.truth.api.IDocGenerator;
import factorization.truth.word.ClipboardWord;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TesrViewer implements IDocGenerator {
    @Override
    public void process(AbstractTypesetter out, String arg) {
        ArrayList<Class> cs = new ArrayList<Class>(TileEntityRendererDispatcher.instance.mapSpecialRenderers.keySet());
        Collections.sort(cs, new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                return o1.getCanonicalName().compareTo(o2.getCanonicalName());
            }
        });
        out.append("\\title{TESRs}\n\n");
        for (Class c : cs) {
            out.append("\n\n" + c.getCanonicalName() + " ");
            out.emitWord(new ClipboardWord("/scrap DeregisterTesr " + c.getCanonicalName()));
        }
    }
}
