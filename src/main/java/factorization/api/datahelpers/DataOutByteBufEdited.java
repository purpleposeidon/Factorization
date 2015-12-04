package factorization.api.datahelpers;

import java.io.DataOutput;

import net.minecraftforge.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class DataOutByteBufEdited extends DataOutByteBuf {

    public DataOutByteBufEdited(ByteBuf buf) {
        super(buf, Side.CLIENT);
    }

    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public && share.client_can_edit;
    }
}
