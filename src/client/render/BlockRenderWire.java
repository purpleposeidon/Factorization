package factorization.client.render;

import factorization.api.Coord;
import factorization.api.VectorUV;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.TileEntityWire;
import factorization.common.WireConnections;
import net.minecraft.client.Minecraft;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;

public class BlockRenderWire extends FactorizationBlockRender {
    RenderingCube bot = new RenderingCube(11, new VectorUV(1, 0, 0), new VectorUV(0, -1, 0));
    @Override
    void render(RenderBlocks rb) {
        if (world_mode) {
            Tessellator.instance.setBrightness(Core.registry.factory_block.getMixedBrightnessForBlock(w, x, y, z));
            Coord me = new Coord(Minecraft.getMinecraft().theWorld, x, y, z);
            for (RenderingCube rc : new WireConnections(me.getTE(TileEntityWire.class)).getParts()) {
                renderCube(rc);
            }
        } else {
            for (RenderingCube rc : WireConnections.getInventoryParts()) {
                renderCube(rc);
            }
//			renderCube(bot);
//			renderCube(new RenderingCube(11, new Vector(-16, -16, -16), null));
//			int lead = Core.registry.resource_block.getBlockTextureFromSideAndMetadata(0, Core.registry.lead_block_item.getItemDamage());
//			float wireWidth = 4F / 16F;
//			renderPart(rb, lead, 0, 0, 0, 1, wireWidth, wireWidth);
//			renderPart(rb, lead, 0, 0, 0, wireWidth, wireWidth, 1);
        }
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.LEADWIRE;
    }
}
