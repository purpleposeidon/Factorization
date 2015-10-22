package factorization.truth.gen;

import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import factorization.truth.word.ClipboardWord;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TesrViewer implements IDocGenerator {
    @Override
    public void process(ITypesetter out, String arg) throws TruthError {
        ArrayList<Class> cs = new ArrayList<Class>(TileEntityRendererDispatcher.instance.mapSpecialRenderers.keySet());
        Collections.sort(cs, new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                return o1.getCanonicalName().compareTo(o2.getCanonicalName());
            }
        });
        out.write("\\title{TESRs}\n\n");
        for (Class c : cs) {
            out.write("\n\n" + c.getCanonicalName() + " ");
            out.write(new ClipboardWord("/scrap DeregisterTesr " + c.getCanonicalName()));
        }
    }
}
