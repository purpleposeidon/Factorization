package factorization.docs;

import net.minecraft.util.EnumChatFormatting;

public class Word {
    final String text;
    final String hyperlink; //NORELEASE: str
    
    public Word(String text, String hyperlink) {
        this.hyperlink = hyperlink;
        if (hyperlink == null) {
            this.text = text;
        } else {
            this.text = "" + EnumChatFormatting.AQUA  + EnumChatFormatting.UNDERLINE+ text;
        }
    }
    
    @Override
    public String toString() {
        return text + " ==> " + hyperlink;
    }
}
