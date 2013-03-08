package factorization.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import factorization.api.Coord;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.TileEntityWire;
import factorization.common.WireConnections;
import factorization.common.WireRenderingCube;

public class BlockRenderWire extends FactorizationBlockRender {
    @Override
    void render(RenderBlocks rb) {
        if (world_mode) {
            Tessellator.instance.setBrightness(Core.registry.factory_block.getMixedBrightnessForBlock(w, x, y, z));
            Coord me = new Coord(Minecraft.getMinecraft().theWorld, x, y, z);
            for (WireRenderingCube rc : new WireConnections(me.getTE(TileEntityWire.class)).getParts()) {
                renderCube(rc);
            }
        } else {
            for (WireRenderingCube rc : WireConnections.getInventoryParts()) {
                renderCube(rc);
            }
        }
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.LEADWIRE;
    }
}
