package factorization.truth.gen;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import factorization.truth.AbstractTypesetter;

import java.util.List;

public class WorldgenViewer implements IDocGenerator {

    @Override
    public void process(AbstractTypesetter out, String arg) {
        try {
            GameRegistry.generateWorld(0, 0, null, null, null);
        } catch (NullPointerException e) {
            // lazy way of making the sortedGeneratorList not be null. Swallow the exception whole.
        }
        List<IWorldGenerator> sortedGeneratorList = ReflectionHelper.getPrivateValue(GameRegistry.class, null, "sortedGeneratorList");
        out.append("\\title{Sorted World Generators}\n\n");
        if (sortedGeneratorList == null) {
            out.append("Failed to load generator list!");
            return;
        }

        for (IWorldGenerator gen : sortedGeneratorList) {
            out.append(gen.toString() + "\n\n");
        }
    }
}
