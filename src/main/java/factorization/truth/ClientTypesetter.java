package factorization.truth;

import net.minecraft.client.gui.FontRenderer;

public class ClientTypesetter extends AbstractTypesetter {

    public ClientTypesetter(String domain, FontRenderer font, int pageWidth, int pageHeight) {
        super(domain, font, pageWidth, pageHeight - WordPage.TEXT_HEIGHT * 2);
    }

}
