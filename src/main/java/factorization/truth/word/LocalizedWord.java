package factorization.truth.word;

import factorization.util.LangUtil;

public class LocalizedWord extends TextWord {
    public LocalizedWord(String text) {
        super(LangUtil.translateThis(text));
    }
}
