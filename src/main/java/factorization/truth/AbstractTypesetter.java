package factorization.truth;

import com.google.common.base.Strings;
import factorization.truth.api.*;

import java.util.Locale;

public abstract class AbstractTypesetter implements ITypesetter {
    // Super-awesome typesetter version π², by neptunepink
    public final String domain;
    
    public AbstractTypesetter(String domain) {
        this.domain = domain;
    }

    @Override
    public String getVariable(String name) {
        return DocReg.getVariable(name);
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public void write(final String text) throws TruthError {
        if (Strings.isNullOrEmpty(text)) return;
        final Tokenizer tokenizer = new Tokenizer(text);
        
        while (tokenizer.nextToken()) {
            final String token = tokenizer.token;
            if (token.isEmpty()) continue;
            switch (tokenizer.type) {
            default:
                throw new TruthError("Unknown tokentype: " + tokenizer.token);
            case WORD:
                runWord(token);
                break;
            case PARAMETER:
                write(token);
                break;
            case COMMAND:
                final String cmdName = token.toLowerCase(Locale.ROOT);
                ITypesetCommand cmd = DocReg.commands.get(cmdName);
                if (cmd == null) {
                    throw new TruthError("Unknown command: " + cmdName);
                }
                runCommand(cmd, tokenizer);
                break;
            }
        }
    }

    public abstract void writeErrorMessage(String msg);
    protected abstract void runWord(String word);
    protected abstract void runCommand(ITypesetCommand cmd, ITokenizer tokenizer) throws TruthError;
}
