package factorization.client.gui;

import factorization.common.ContainerFactorization;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

public abstract class FactorizationGui extends GuiContainer {
    protected ContainerFactorization factContainer;

    public FactorizationGui(ContainerFactorization container) {
        super(container);
        factContainer = container;
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int foo, int bar) {
        fontRenderer.drawString(factContainer.factory.getInvName(), 8, 6, 0x404040);
        InventoryPlayer ip = factContainer.entityplayer.inventory;
        this.fontRenderer.drawString(ip.isInvNameLocalized() ? ip.getInvName() : I18n.getString(ip.getInvName()), 8, this.ySize - 96 + 2, 4210752);
    }
    
    

}
