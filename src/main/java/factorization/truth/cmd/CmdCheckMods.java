package factorization.truth.cmd;

import factorization.truth.api.AgnosticCommand;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import net.minecraftforge.fml.common.Loader;

public class CmdCheckMods extends AgnosticCommand {
    @Override
    protected void call(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String mode = tokenizer.getParameter("\\checkmods mod mode: all|none|any"); // all some none
        String modList = tokenizer.getParameter("\\checkmods list of mods"); //craftguide NotEnoughItems
        String content = tokenizer.getParameter("\\checkmods when mods installed");
        String other = tokenizer.getParameter("\\checkmods when mods not installed");
        int count = 0;
        String[] mods = modList.split(" ");
        for (String modId : mods) {
            if (Loader.isModLoaded(modId)) {
                count++;
            }
        }
        boolean good = false;
        if (mode.equalsIgnoreCase("all")) {
            good = count == mods.length;
        } else if (mode.equalsIgnoreCase("none")) {
            good = count == 0;
        } else if (mode.equalsIgnoreCase("any")) {
            good = count > 1;
        } else {
            throw new TruthError("\\checkmods first parameter must be 'all', 'none', or 'any', not '" + mode + "'");
        }
        if (good) {
            out.write(content);
        } else if (other != null) {
            out.write(other);
        }
    }
}
