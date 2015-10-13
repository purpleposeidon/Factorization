package factorization.truth.api;

public interface ITypesetCommand {
    void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError;
    void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError;
}
