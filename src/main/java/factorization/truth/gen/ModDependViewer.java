package factorization.truth.gen;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;

public class ModDependViewer implements IDocGenerator {
    @Override
    public void process(ITypesetter out, String arg) throws TruthError {
        if ("".equals(arg)) listMods(out);
        else showMod(out, arg);
    }

    private void showMod(ITypesetter out, String arg) throws TruthError {
        ModContainer mod = null;
        for (ModContainer it : Loader.instance().getActiveModList()) {
            if (it.getModId().equals(arg)) {
                mod = it;
                break;
            }
        }
        if (mod == null) {
            out.write("Mod not found: " + arg);
            return;
        }
        out.write(String.format("\\title{%s}\n\n", mod.getName()));
        out.write("Modid: " + arg + "\n\n");
        if (!mod.getDependencies().isEmpty()) {
            out.write("\\b{Dependencies}\n\n");
            for (ArtifactVersion version : mod.getDependencies()) {
                String link = String.format("\\link{cgi/mods/%s}{%s}", version.getLabel(), version.getLabel());
                out.write(link + ": " + version.getRangeString() + "\\nl");
            }
        }
        if (!mod.getDependants().isEmpty()) {
            out.write("\\b{Dependents}\n\n");
            for (ArtifactVersion version : mod.getDependants()) {
                String link = String.format("\\link{cgi/mods/%s}{%s}", version.getLabel(), version.getLabel());
                out.write(link + ": " + version.getRangeString() + "\\nl");
            }
        }
    }

    private void listMods(ITypesetter out) throws TruthError {
        out.write("\\title{Installed Mods}\n\n");
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            out.write(String.format("\\link{cgi/mods/%s}{%s}\n\n", mod.getModId(), mod.getName()));
        }
    }
}
