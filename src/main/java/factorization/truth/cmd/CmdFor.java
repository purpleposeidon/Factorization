package factorization.truth.cmd;

import factorization.truth.api.AgnosticCommand;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;

public class CmdFor extends AgnosticCommand {
    @Override
    protected void call(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String varname = tokenizer.getParameter("for variable name");
        String body = tokenizer.getParameter("for loop body");
        String varVal = out.getVariable(varname);
        if (varVal.isEmpty()) return;
        for (String part : varVal.split("\n")) {
            out.write(body.replaceAll("%1", part));
        }
    }
}
