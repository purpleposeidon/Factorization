package factorization.truth.cmd;

import factorization.truth.api.*;

public class CmdNewpage implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.newPage();
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        // NOP
    }
}
