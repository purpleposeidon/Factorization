package factorization.truth.gen;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;

import java.util.List;

public class WorldgenViewer implements IDocGenerator {

    @Override
    public void process(ITypesetter out, String arg) throws TruthError {
        try {
            GameRegistry.generateWorld(0, 0, null, null, null);
        } catch (NullPointerException e) {
            // lazy way of making the sortedGeneratorList not be null. Swallow the exception whole.
        }
        List<IWorldGenerator> sortedGeneratorList = ReflectionHelper.getPrivateValue(GameRegistry.class, null, "sortedGeneratorList");
        out.write("\\title{Sorted World Generators}\n\n");
        if (sortedGeneratorList == null) {
            out.write("Failed to load generator list!");
            return;
        }

        for (IWorldGenerator gen : sortedGeneratorList) {
            out.write(gen.toString() + "\n\n");
        }
    }
}
