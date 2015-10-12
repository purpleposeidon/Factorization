package factorization.truth.api;

public interface IDocGenerator {
    void process(ITypesetter out, String arg) throws TruthError;
}
