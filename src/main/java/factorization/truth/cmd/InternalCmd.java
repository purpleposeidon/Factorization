package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.ITypesetCommand;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;

public abstract class InternalCmd implements ITypesetCommand {
    @Override
    public final void callClient(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        callClient((ClientTypesetter) out, tokenizer);
    }

    @Override
    public final void callHTML(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        callHtml((HtmlConversionTypesetter) out, tokenizer);
    }

    protected abstract void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError;
    protected abstract void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError;
}
