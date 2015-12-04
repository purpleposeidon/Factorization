package factorization.coremodhooks;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class UnhandledGuiKeyEvent extends Event {
    public final char chr;
    public final int keysym;
    public final EntityPlayer player;
    public final GuiScreen gui;

    public UnhandledGuiKeyEvent(char chr, int keysym, EntityPlayer player, GuiScreen gui) {
        this.chr = chr;
        this.keysym = keysym;
        this.player = player;
        this.gui = gui;
    }
}
