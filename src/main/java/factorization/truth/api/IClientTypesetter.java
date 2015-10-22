package factorization.truth.api;

import factorization.truth.word.Word;

import java.util.List;

public interface IClientTypesetter extends ITypesetter {
    /**
     * Send a word directly to the typesetter. The contents of the word will not be word-wrapped.
     * @param w the Word
     */
    void write(IWord w);

    /**
     * Similar to {@link ITypesetter#write(String)}, but the text will also have the link/style applied.
     * @param text Text of the word
     * @param link The link
     * @param style The style;
     * @see net.minecraft.util.EnumChatFormatting
     */
    void write(String text, String link, String style) throws TruthError;

    AbstractPage getCurrentPage();

    List<AbstractPage> getPages();

    void newPage();

    void addPage(AbstractPage page);

    int getPageWidth();

    int getPageHeight();
}
