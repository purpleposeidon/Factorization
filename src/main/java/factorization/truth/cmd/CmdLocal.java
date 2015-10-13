package factorization.truth.cmd;

import factorization.shared.Core;
import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;
import factorization.truth.word.LocalizedWord;

public class CmdLocal extends InternalCmd {
    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String localizationKey = tokenizer.getParameter("localization key");
        out.write(new LocalizedWord(localizationKey, out.getInfo().link));
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        String localizationKey = tokenizer.getParameter("localization key");
        out.html(Core.translate(localizationKey));
    }
}
