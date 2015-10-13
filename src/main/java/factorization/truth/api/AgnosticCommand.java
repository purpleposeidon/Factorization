package factorization.truth.api;

public abstract class AgnosticCommand implements ITypesetCommand {
    protected abstract void call(ITypesetter out, ITokenizer tokenizer) throws TruthError;

    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        call(out, tokenizer);
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        call(out, tokenizer);
    }
}
