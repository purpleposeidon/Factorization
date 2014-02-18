package factorization.docs;

import net.minecraft.util.EnumChatFormatting;

public class Word {
    final String text;
    final Object hyperlink;
    
    public Word(String text, Object hyperlink) {
        if (hyperlink == null) {
            this.text = text;
        } else {
            this.text = "" + EnumChatFormatting.AQUA  + EnumChatFormatting.UNDERLINE+ text;
        }
        this.hyperlink = hyperlink;
    }
}
