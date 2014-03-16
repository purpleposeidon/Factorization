package factorization.charge;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderWire extends FactorizationBlockRender {
    @Override
    public boolean render(RenderBlocks rb) {
        if (world_mode) {
            Tessellator.instance.setBrightness(Core.registry.factory_block.getMixedBrightnessForBlock(w, x, y, z));
            if (te == null) {
                return false;
            }
            for (WireRenderingCube rc : new WireConnections((TileEntityWire) te).getParts()) {
                renderCube(rc);
            }
        } else {
            for (WireRenderingCube rc : WireConnections.getInventoryParts()) {
                renderCube(rc);
            }
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LEADWIRE;
    }
}
