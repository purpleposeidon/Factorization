package factorization.docs;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.ArtifactVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class ModDependViewer implements IDocGenerator {
    @Override
    public void process(AbstractTypesetter out, String arg) {
        if ("".equals(arg)) listMods(out);
        else showMod(out, arg);
    }

    private void showMod(AbstractTypesetter out, String arg) {
        ModContainer mod = null;
        String humanName;
        for (ModContainer it : Loader.instance().getActiveModList()) {
            if (it.getModId().equals(arg)) {
                mod = it;
                break;
            }
        }
        if (mod == null) {
            out.append("Mod not found: " + arg);
            return;
        }
        out.append(String.format("\\title{%s}\n\n", mod.getName()));
        out.append("Modid: " + arg + "\n\n");
        if (!mod.getDependencies().isEmpty()) {
            out.append("\\b{Dependencies}\n\n");
            for (ArtifactVersion version : mod.getDependencies()) {
                String link = String.format("\\link{cgi/mods/%s}{%s}", version.getLabel(), version.getLabel());
                out.append(link + ": " + version.getRangeString() + "\\nl");
            }
        }
        if (!mod.getDependants().isEmpty()) {
            out.append("\\b{Dependents}\n\n");
            for (ArtifactVersion version : mod.getDependants()) {
                String link = String.format("\\link{cgi/mods/%s}{%s}", version.getLabel(), version.getLabel());
                out.append(link + ": " + version.getRangeString() + "\\nl");
            }
        }
    }

    private void listMods(AbstractTypesetter out) {
        out.append("\\title{Installed Mods}\n\n");
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            out.append(String.format("\\link{cgi/mods/%s}{%s}\n\n", mod.getModId(), mod.getName()));
        }
    }
}
