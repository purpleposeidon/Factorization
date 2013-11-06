package factorization.api.datahelpers;

import java.io.DataInput;

import cpw.mods.fml.relauncher.Side;

public class DataInPacketClientEdited extends DataInPacket {

    public DataInPacketClientEdited(DataInput dis) {
        super(dis, Side.CLIENT);
    }
    
    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public && share.client_can_edit;
    }

}
