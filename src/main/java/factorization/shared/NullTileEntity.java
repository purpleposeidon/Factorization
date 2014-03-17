package factorization.shared;

import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.shared.NetworkFactorization.MessageType;

public class NullTileEntity extends TileEntity {
    int description_request_delay = 5;
    int fails = 1;
    
    @Override
    public void updateEntity() {
        if (FMLCommonHandler.instance().getEffectiveSide() != Side.CLIENT) {
            new Coord(this).rmTE();
        }
        if (description_request_delay-- == 0) {
            Coord here = new Coord(this);
            Core.logFine("%s: asking for description packet", here);
            description_request_delay = 20*fails;
            Core.network.broadcastMessage(null, here, MessageType.DescriptionRequest);
            fails++;
        }
    }
}
