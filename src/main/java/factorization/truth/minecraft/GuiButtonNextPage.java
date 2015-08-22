package factorization.truth.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiButtonNextPage extends GuiButton {
    /**
     * True for pointing right (next page), false for pointing left (previous
     * page).
     */
    private final boolean nextPage;
    private static final ResourceLocation bookGuiTextures = new ResourceLocation("factorization:textures/gui/book.png");
    
    public GuiButtonNextPage(int id, int posX, int posY, boolean next) {
        super(id, posX, posY, 23, 13, "");
        this.nextPage = next;
    }

    /**
     * Draws this button to the screen.
     */
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        boolean flag = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width
                && mouseY < this.yPosition + this.height;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(bookGuiTextures);
        int k = 0;
        int l = 192;

        if (flag) {
            k += 23;
        }

        if (!this.nextPage) {
            l += 13;
        }

        this.drawTexturedModalRect(this.xPosition, this.yPosition, k, l, 23, 13);
    }
}