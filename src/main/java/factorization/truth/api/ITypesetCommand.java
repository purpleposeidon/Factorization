package factorization.truth.api;

public interface ITypesetCommand {
    void callClient(ITypesetter out, ITokenizer tokenizer) throws TruthError;
    void callHTML(ITypesetter out, ITokenizer tokenizer) throws TruthError;
}
