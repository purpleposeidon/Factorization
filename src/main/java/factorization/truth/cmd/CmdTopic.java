package factorization.truth.cmd;

import factorization.truth.api.AgnosticCommand;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import factorization.truth.export.IndexDocumentation;
import factorization.truth.export.IndexerTypesetter;

public class CmdTopic extends AgnosticCommand {
    @Override
    protected void call(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String topic = tokenizer.getParameter("topic name");
        String sub = String.format("\\newpage \\generate{recipes/for/%s}", topic);
        out.write(sub);
        if (out instanceof IndexerTypesetter) {
            ((IndexerTypesetter) out).foundTopic(topic);
        }
    }
}
