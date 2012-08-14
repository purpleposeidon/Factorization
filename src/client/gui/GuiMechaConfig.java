package factorization.client.gui;

import java.util.ArrayList;

import net.minecraft.src.Block;
import net.minecraft.src.Container;
import net.minecraft.src.GameSettings;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemArmor;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;

import org.lwjgl.opengl.GL11;

import factorization.api.IMechaUpgrade;
import factorization.client.FactorizationClientProxy;
import factorization.common.Command;
import factorization.common.ContainerMechaModder;
import factorization.common.Core;
import factorization.common.MechaArmor;
import factorization.common.MechaArmor.MechaMode;

public class GuiMechaConfig extends GuiContainer {
    ContainerMechaModder cont;

    public GuiMechaConfig(Container cont) {
        super(cont);
        this.cont = (ContainerMechaModder) cont;
        xSize = 175;
        ySize = 197;
    }

    @Override
    public void initGui() {
        super.initGui();
        updateButtons();
    }

    ArrayList<GuiButton> buttons = new ArrayList<GuiButton>();

    void updateButtons() {
        buttons.clear();
        ItemStack armor = cont.upgrader.armor;
        if (armor == null) {
            return;
        }
        Item i = armor.getItem();
        if (!(i instanceof MechaArmor)) {
            return;
        }
        MechaArmor m = (MechaArmor) i;
        int left = guiLeft + 27;
        int top = guiTop + 26;
        int size = 16;
        for (int slot = 0; slot < m.slotCount; slot++) {
            if (!m.isValidUpgrade(cont.upgrader.upgrades[slot + 1])) {
                continue;
            }
            buttons.add(new GuiButton(slot * 2, left + slot * (size + 2), top, size, size + 4, "A"));
            if (!m.getSlotMechaMode(armor, slot).isConstant()) {
                buttons.add(new GuiButton(slot * 2 + 1, left + slot * (size + 2), top + 22, size, size + 4, "K"));
            }
        }
    }

    MechaArmor getArmor() {
        if (cont.upgrader.armor == null) {
            return null;
        }
        if (cont.upgrader.armor.getItem() instanceof ItemArmor) {
            return (MechaArmor) cont.upgrader.armor.getItem();
        }
        return null;
    }

    void drawSlotInfo(int slot) {
        MechaArmor armor = getArmor();
        if (armor == null) {
            return;
        }
        ItemStack upgrade = cont.upgrader.getStackInSlot(101 + slot);
        if (upgrade == null) {
            return;
        }

        int left = guiLeft + 8;
        int top = guiTop + 72;
        int delta = 12;
        String description = null;
        if (upgrade.getItem() instanceof IMechaUpgrade) {
            IMechaUpgrade up = (IMechaUpgrade) upgrade.getItem();
            description = up.getDescription();
        }
        else if (upgrade.getItem() instanceof ItemBlock) {
            ItemBlock ib = (ItemBlock) upgrade.getItem();
            if (ib.getBlockID() == Block.cloth.blockID) {
                description = "Emits colored particles";
            }
        }
        if (description == null) {
            return;
        }
        fontRenderer.drawString(description, left, top, 4210752);
        MechaMode mode = armor.getSlotMechaMode(cont.upgrader.armor, slot);
        String action = "";
        if (mode.key >= 0) {
            int key = ((FactorizationClientProxy) Core.proxy).mechas[mode.key].keyCode;
            action = GameSettings.getKeyDisplayString(key) + " is pressed";
        }
        else {
            action = Core.ExtraKey.fromInt(mode.key).text;
        }
        fontRenderer.drawString(mode.mode.describe(action), left, top + delta, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        int k = mc.renderEngine.getTexture(Core.texture_dir + "mechamodder.png");
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(k);
        int l = (width - xSize) / 2;
        int i1 = (height - ySize) / 2;
        drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);

        updateButtons();
        for (GuiButton button : buttons) {
            button.drawButton(mc, var2, var3);
            if (button.id % 2 == 0) {
                if (button.mousePressed(mc, var2, guiTop + 26 + 1)) {
                    if (var3 > guiTop && var3 < guiTop + 20 * 4) {
                        drawSlotInfo(button.id / 2);
                    }
                }
            }
        }
    }

    protected void actionPerformed(GuiButton button, boolean leftClick) {
        if (cont.upgrader.armor == null) {
            return;
        }
        if (!(cont.upgrader.armor.getItem() instanceof MechaArmor)) {
            return;
        }
        (leftClick ? Command.mechaModLeftClick : Command.mechaModRightClick).call(ModLoader.getMinecraftInstance().thePlayer, (byte) button.id);
    }

    @Override
    protected void mouseClicked(int x, int y, int mouseButton) {
        super.mouseClicked(x, y, mouseButton);
        for (GuiButton button : buttons) {
            if (button.mousePressed(mc, x, y)) {
                actionPerformed(button, mouseButton == 0);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer() {
        super.drawGuiContainerForegroundLayer();
        //this.fontRenderer.drawString("Mecha-Modder", 7, 26, 4210752);
    }

}
