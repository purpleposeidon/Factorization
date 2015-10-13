package factorization.truth.cmd;

import factorization.truth.api.*;

public class CmdLeftpage implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        int need = 1 + (out.getPages().size() % 2);
        for (int i = 0; i < need; i++) {
            out.newPage();
        }
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        // NOP
    }
}
