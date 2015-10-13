package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;

public class CmdNl extends InternalCmd {
    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.getCurrentPage().nl();
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.html("<br>\n");
    }
}
