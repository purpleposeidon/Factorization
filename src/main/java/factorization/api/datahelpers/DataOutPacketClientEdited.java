package factorization.api.datahelpers;

import java.io.DataOutput;

import cpw.mods.fml.relauncher.Side;

public class DataOutPacketClientEdited extends DataOutPacket {

    public DataOutPacketClientEdited(DataOutput dos) {
        super(dos, Side.CLIENT);
    }

    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public && share.client_can_edit;
    }
}
