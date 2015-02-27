package factorization.api.datahelpers;

import java.io.DataInput;

import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class DataInByteBufClientEdited extends DataInByteBuf {

    public DataInByteBufClientEdited(ByteBuf dis) {
        super(dis, Side.CLIENT);
    }
    
    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public && share.client_can_edit;
    }

}
