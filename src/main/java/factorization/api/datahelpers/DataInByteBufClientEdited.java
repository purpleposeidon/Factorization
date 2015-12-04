package factorization.api.datahelpers;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.relauncher.Side;

public class DataInByteBufClientEdited extends DataInByteBuf {

    public DataInByteBufClientEdited(ByteBuf dis) {
        super(dis, Side.CLIENT);
    }
    
    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public && share.client_can_edit;
    }

}
