package factorization.artifact;

import factorization.shared.Core;
import factorization.shared.NetworkFactorization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class GuiArtifactForge extends GuiContainer {
    ContainerForge container;
    GuiTextField name_field, lore_field;

    public GuiArtifactForge(ContainerForge container) {
        super(container);
        this.container = container;
        xSize = 256;
        ySize = 237;
    }

    GuiTextField makeField(int x, int y, int width, GuiTextField orig, int text_length) {
        String text = orig == null ? "" : orig.getText();
        int cornerX = (this.width - this.xSize) / 2;
        int cornerY = (this.height - this.ySize) / 2;
        GuiTextField ret = new GuiTextField(this.fontRendererObj, cornerX + x, cornerY + y, width, 12);
        ret.setTextColor(-1);
        ret.setDisabledTextColour(-1);
        ret.setEnableBackgroundDrawing(true);
        ret.setMaxStringLength(text_length);
        ret.setText(text);
        return ret;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        //name_field = makeField(62, 24, 103);
        //lore_field = makeField(62, 64, 103);
        name_field = makeField(143, 28, 103, name_field, 40);
        lore_field = makeField(143, 56, 103, lore_field, 40 * 4);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char c, int sym) {
        if (name_field.textboxKeyTyped(c, sym) || lore_field.textboxKeyTyped(c, sym)) {
            container.forge.markDirty();
            syncFields();
        } else {
            super.keyTyped(c, sym);
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        name_field.mouseClicked(mouseX, mouseY, mouseButton);
        lore_field.mouseClicked(mouseX, mouseY, mouseButton);
    }

    void syncFields() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        Core.network.sendPlayerMessage(player, NetworkFactorization.MessageType.ArtifactForgeName, name_field.getText(), lore_field.getText());
    }

    private static final ResourceLocation bgTexture = Core.getResource("textures/gui/artifactforge.png");

    @Override
    public void drawDefaultBackground() {
        super.drawDefaultBackground();
        double r = 59;
        double dTheta = Math.PI * 2 / container.enchantSlots.size();
        int i = 0;
        double speed = Math.PI * 2 / 10;
        // 75
        int cx = 60, cy = 67;
        for (Slot slot : container.enchantSlots) {
            double theta = dTheta * i++ + speed * System.currentTimeMillis() / 1000;
            int x = (int) (cx + Math.cos(theta) * r);
            int y = (int) (cy + Math.sin(theta) * r);
            slot.xDisplayPosition = x;
            slot.yDisplayPosition = y;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(bgTexture);
        int halfWidth = (width - xSize) / 2;
        int halfHeight = (height - ySize) / 2;
        final int x0 = halfWidth - 4;
        final int y0 = halfHeight - 19 + 16;
        drawTexturedModalRect(x0, y0, 0, 0, xSize, ySize);
        int i = InventoryForge.SLOT_ENCHANT_START;
        for (Slot slot : container.enchantSlots) {
            int dx = container.forge.warnings[i] == 1 ? 18 : 0;
            drawTexturedModalRect(x0 + slot.xDisplayPosition + 3, y0 + slot.yDisplayPosition + 2, dx, 238, 18, 18);
            i++;
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPushMatrix();
        //GL11.glTranslatef(guiLeft, guiTop, 0.0F);
        if (name_field != null) name_field.drawTextBox();
        if (lore_field != null) lore_field.drawTextBox();
        GL11.glPopMatrix();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int p_146979_1_, int p_146979_2_) {
        //fontRendererObj.drawString(I18n.format("factorization:container.artifactforge"), 28, 0, 4210752);
        //fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 - 14, 4210752);
        fontRendererObj.drawString(I18n.format("factorization.forge.name"), 156, 16, 4210752);
        fontRendererObj.drawString(I18n.format("factorization.forge.lore"), 156, 45, 4210752);
        String err = container.forge.error_message;
        if (err == null) err = "lorehint";
        int dy = 10;
        int y = 65;
        int i = 1;
        while (true) {
            String key = "factorization.forge.err." + err + "." + i;
            String msg = Core.translateExact(key);
            if (msg == null) {
                if (i > 1) break;
                msg = key;
            }
            fontRendererObj.drawString(msg, 138, y + dy * i, 4210752);
            if (i++ > 10) break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partial) {
        super.drawScreen(mouseX, mouseY, partial);
    }
}
