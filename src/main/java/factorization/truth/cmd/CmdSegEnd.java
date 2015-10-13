package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;

public class CmdSegEnd extends InternalCmd {
    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.segmentStart = null;
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {

    }
}
