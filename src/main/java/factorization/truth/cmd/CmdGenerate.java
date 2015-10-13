package factorization.truth.cmd;

import factorization.truth.api.*;

public class CmdGenerate extends AgnosticCommand {
    @Override
    protected void call(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String arg = tokenizer.getParameter("\\generate path");
        String args[] = arg.split("/", 2);
        IDocGenerator gen = DocReg.generators.get(args[0]);
        if (gen == null) {
            throw new TruthError("\\generate{" + arg + "}: Not found: " + args[0]);
        }
        String rest = args.length > 1 ? args[1] : "";
        gen.process(out, rest);
    }
}
