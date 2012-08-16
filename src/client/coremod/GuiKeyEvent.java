package factorization.client.coremod;

import net.minecraftforge.event.Cancelable;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.ListenerList;

@Cancelable
public class GuiKeyEvent extends Event {
    public char key;
    public int symbol;

    public GuiKeyEvent(char key, int symbol) {
        this.key = key;
        this.symbol = symbol;
    }

    // NOTE: These functions would be ASM-added if it weren't for this being in
    // coremods...
    // See EventTransformer.buildEvents
    private static ListenerList LISTENER_LIST;

    public GuiKeyEvent() {
        super();
    }

    @Override
    protected void setup() {
        super.setup();
        if (LISTENER_LIST != null) {
            return;
        }
        LISTENER_LIST = new ListenerList(super.getListenerList());
    }
    
    @Override
    public ListenerList getListenerList() {
        return this.LISTENER_LIST;
    }
}
