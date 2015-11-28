package factorization.truth.cmd;

import factorization.truth.api.*;
import factorization.truth.word.LocalizedWord;
import factorization.util.LangUtil;

public class CmdLocal implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String localizationKey = tokenizer.getParameter("localization key");
        out.write(new LocalizedWord(localizationKey));
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        String localizationKey = tokenizer.getParameter("localization key");
        out.html(LangUtil.translate(localizationKey));
    }
}
