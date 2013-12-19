package factorization.ceramics;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import factorization.ceramics.TileEntityGreenware.ClayLump;
import factorization.ceramics.TileEntityGreenware.ClayState;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.FactoryType;

public class BlockRenderGreenware extends FactorizationBlockRender {
    static BlockRenderGreenware instance;
    
    public BlockRenderGreenware() {
        instance = this;
        setup();
    }
    
    private boolean texture_init = false;
    public void setup() {
        if (texture_init) {
            return;
        }
    }
    
    private static TileEntityGreenware loader = new TileEntityGreenware();
    
    @Override
    public void render(RenderBlocks rb) {
        if (!world_mode) {
            if (is == null) {
                return;
            }
            BlockRenderHelper block = BlockRenderHelper.instance;
            boolean stand = true;
            boolean rescale = false;
            if (is.hasTagCompound()) {
                loader.loadParts(is.getTagCompound());
                int minX = 32, minY = 32, minZ = 32;
                int maxX = 0, maxY = 32, maxZ = 32;
                for (ClayLump cl : loader.parts) {
                    minX = Math.min(minX, cl.minX);
                    minY = Math.min(minY, cl.minY);
                    minZ = Math.min(minZ, cl.minZ);
                    maxX = Math.max(maxX, cl.maxX);
                    maxY = Math.max(maxY, cl.maxY);
                    maxZ = Math.max(maxZ, cl.maxZ);
                    int min = Math.min(Math.min(minX, minY), minZ);
                    int max = Math.max(Math.max(maxX, maxY), maxZ);
                    if (min < 16 || max > 32) {
                        rescale = true;
                        break;
                    }
                }
                if (rescale) {
                    GL11.glPushMatrix();
                    float scale = 1F/3F;
                    GL11.glScalef(scale, scale, scale);
                }
                renderDynamic(loader);
                ClayState cs = loader.getState();
                stand = cs == ClayState.WET || cs == ClayState.DRY;
            } else {
                setupRenderGenericLump().renderForInventory(rb);
            }
            if (stand) {
                setupRenderStand().renderForInventory(rb);
            }
            if (rescale) {
                GL11.glPopMatrix();
            }
            return;
        }
        TileEntityGreenware gw = (TileEntityGreenware) te;
        if (gw == null) {
            return;
        }
        if (world_mode) {
            Tessellator.instance.setBrightness(Core.registry.factory_block.getMixedBrightnessForBlock(w, x, y, z));
        }
        ClayState state = gw.getState();
        if (state == ClayState.DRY || state == ClayState.WET) {
            BlockRenderHelper block = setupRenderStand();
            block.render(rb, x, y, z);
        }
        if (!gw.canEdit()) {
            renderStatic(gw);
        }
        gw.shouldRenderTesr = state == ClayState.WET;
    }
    
    private static Random rawMimicRandom = new Random();
    
    int getColor(ClayLump rc) {
        if (rc.raw_color == -1) {
            //Get the raw color, possibly making something up
            if (rc.icon_id == Core.registry.resource_block.blockID && rc.icon_md > 16) {
                for (BasicGlazes bg : BasicGlazes.values()) {
                    if (bg.metadata == rc.icon_md) {
                        if (bg.raw_color == -1) {
                            bg.raw_color = 0xFF00FF;
                        }
                        rc.raw_color = bg.raw_color;
                        break;
                    }
                }
            }
            if (rc.raw_color == -1) {
                rawMimicRandom.setSeed((rc.icon_id << 16) + rc.icon_md);
                int c = 0;
                for (int i = 0; i < 3; i++) {
                    c += (rawMimicRandom.nextInt(0xE0) + 10);
                    c <<= 16;
                }
                rc.raw_color = c;
            }
        }
        return rc.raw_color;
    }
    
    private boolean spammed = false;
    
    void renderToTessellator(TileEntityGreenware greenware) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        ClayState state = greenware.getState();
        if (state != ClayState.HIGHFIRED) {
            switch (state) {
            case WET: block.useTexture(Block.blockClay.getBlockTextureFromSide(0)); break;
            case DRY: block.useTexture(BlockIcons.ceramics$dry); break;
            case BISQUED: block.useTexture(BlockIcons.ceramics$bisque); break;
            case UNFIRED_GLAZED: block.useTexture(BlockIcons.ceramics$rawglaze); break;
            default: block.useTexture(BlockIcons.error); break;
            }
        }
        boolean colors_changed = false;
        for (ClayLump rc : greenware.parts) {
            if (state == ClayState.HIGHFIRED) {
                Block it = Block.blocksList[rc.icon_id];
                if (it == null) {
                    block.useTexture(BlockIcons.error);
                } else {
                    for (int i = 0; i < 6; i++) {
                        int useIcon = i;
                        if (rc.icon_side == -1) {
                            block.setTexture(i, it.getIcon(useIcon, rc.icon_md));
                        } else {
                            useIcon = rc.icon_side;
                            block.useTexture(it.getIcon(useIcon, rc.icon_md));
                        }
                        int color = 0xFFFFFF; 
                        if (greenware.worldObj != null) {
                            try {
                                color = it.colorMultiplier(greenware.worldObj, greenware.xCoord, greenware.yCoord, greenware.zCoord);
                            } catch (Throwable t) {
                                if (!spammed) {
                                    spammed = true;
                                    Core.logWarning("%s: could not get a Block.colorMultiplier from %s", greenware.getCoord(), it);
                                    t.printStackTrace();
                                }
                            }
                        } else {
                            color = it.getRenderColor(useIcon);
                        }
                        if (color != 0xFFFFFF) {
                            colors_changed = true;
                            if (rc.icon_side == -1) {
                                block.setColor(i, color);
                            } else {
                                block.setColor(color);
                            }
                        }
                        if (rc.icon_side != -1) {
                            break;
                        }
                    }
                }
            }
            if (state == ClayState.UNFIRED_GLAZED) {
                block.setColor(getColor(rc));
                colors_changed = true;
            }
            rc.toBlockBounds(block);
            block.begin();
            block.rotateMiddle(rc.quat);
            block.renderRotated(Tessellator.instance, x, y, z);
        }
        if (colors_changed) {
            block.resetColors();
        }
    }
    
    void renderDynamic(TileEntityGreenware greenware) {
        Tessellator.instance.startDrawingQuads();
        renderToTessellator(greenware);
        Tessellator.instance.draw();
    }
    
    void renderStatic(TileEntityGreenware greenware) {
        renderToTessellator(greenware);
    }
    
    BlockRenderHelper setupRenderStand() {
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTexture(BlockIcons.ceramics$stand);
        block.setBlockBounds(0, 0, 0, 1, 1F/8F, 1);
        return block;
    }
    
    BlockRenderHelper setupRenderGenericLump() {
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTexture(Block.blockClay.getBlockTextureFromSide(0));
        block.setBlockBounds(3F/16F, 1F/8F, 3F/16F, 13F/16F, 7F/8F, 13F/16F);
        return block;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CERAMIC;
    }

}
