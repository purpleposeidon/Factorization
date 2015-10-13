package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.*;

public class CmdSegEnd implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        ((ClientTypesetter)out).segmentStart = null;
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {

    }
}
