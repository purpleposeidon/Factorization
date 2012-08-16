package factorization.client.coremod;

import net.minecraftforge.event.Cancelable;
import net.minecraftforge.event.Event;

@Cancelable
public class GuiKeyEvent extends Event {
    public char key;
    public int symbol;

    public GuiKeyEvent(char key, int symbol) {
        this.key = key;
        this.symbol = symbol;
    }
}
