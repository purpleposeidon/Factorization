package factorization.shared;

import factorization.api.Coord;
import factorization.net.StandardMessageType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class NullTileEntity extends TileEntity implements ITickable {
    int description_request_delay = 5;
    int fails = 1;
    
    @Override
    public void update() {
        if (FMLCommonHandler.instance().getEffectiveSide() != Side.CLIENT) {
            new Coord(this).rmTE();
        }
        if (description_request_delay-- == 0) {
            Coord here = new Coord(this);
            Core.logFine("%s: asking for description packet", here);
            description_request_delay = 20*fails;
            Core.network.broadcastMessage(null, this, StandardMessageType.DescriptionRequest);
            fails++;
        }
    }
}
