package factorization.truth.cmd;

import factorization.truth.api.AgnosticCommand;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;

public class CmdMacro extends AgnosticCommand {
    int parameter_count = 0;
    final String src;

    public CmdMacro(String src) {
        while (true) {
            if (src.contains("%" + (parameter_count + 1))) {
                parameter_count++;
            } else {
                break;
            }
        }
        this.src = src;
    }

    @Override
    protected void call(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String v = src;
        for (int i = 1; i <= parameter_count; i++) {
            String val = tokenizer.getParameter("Macro parameter %" + i);
            v = v.replace("%" + i, val);
        }
        out.write(v);
    }
}
