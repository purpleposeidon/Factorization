package factorization.truth.cmd;

import factorization.truth.WordPage;
import factorization.truth.api.*;

public class CmdNl implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        ((WordPage)(out.getCurrentPage())).nl();
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.html("<br>\n");
    }
}
