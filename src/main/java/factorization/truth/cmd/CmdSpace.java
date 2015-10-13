package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.*;
import factorization.truth.export.HtmlConversionTypesetter;

public class CmdSpace extends InternalCmd {

    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.write(" ", out.getInfo().link);
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.html(" ");
    }
}
