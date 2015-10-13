package factorization.truth.cmd;

import factorization.truth.api.*;

public class CmdTopic extends AgnosticCommand {
    @Override
    protected void call(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String topic = tokenizer.getParameter("topic name");
        String sub = String.format("\\newpage \\generate{recipes/for/%s}", topic);
        out.write(sub);
    }
}
