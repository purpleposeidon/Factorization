package factorization.coremod;

import com.google.common.eventbus.EventBus;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;

public class BugFixerContainer extends DummyModContainer {
    public BugFixerContainer() {
        super(new ModMetadata());
        ModMetadata md = getMetadata();
        md.modId = getName();
        md.version = getVersion();
        md.description = "";
        md.parent = "factorization";
    }
    
    @Override
    public String getName() {
        return "FzVanillaBugFixer";
    }
    
    @Override
    public String getVersion() {
        return "0";
    }
    
    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        //bus.register(this); -- we don't need access to @PreInit-type-stuff.
        return true;
    }
}
