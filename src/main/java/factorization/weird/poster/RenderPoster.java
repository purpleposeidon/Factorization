package factorization.weird.poster;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class RenderPoster extends RenderEntity {

    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partial) {
        EntityPoster poster = (EntityPoster) ent;
        final Minecraft mc = Minecraft.getMinecraft();
        final MovingObjectPosition mop = mc.objectMouseOver;
        boolean selected = mop != null && mop.entityHit == ent;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        if (selected && !mc.gameSettings.hideGUI) {
            GL11.glPushMatrix();
            // They ordinarily don't move, so no need to bother w/ interpolation
            GL11.glTranslated(-ent.posX, -ent.posY, -ent.posZ);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            RenderGlobal.drawOutlinedBoundingBox(ent.boundingBox, 0);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_ALPHA_TEST); // This should always be enabled; some other mod's derping things up tho; we can leave it on
        GL11.glDisable(GL11.GL_BLEND); // seems to fix that 'no alpha' issue I was having?
        poster.rot.glRotate();
        double s = poster.scale;
        GL11.glScaled(s, s, s);
        try {
            renderItem(poster.inv);
        } catch (Throwable t) {
            t.printStackTrace();
            poster.inv = new ItemStack(Blocks.fire); // Hopefully fire doesn't also somehow error out.
        } finally {
            GL11.glPopAttrib();
        }
        GL11.glPopMatrix();
    }

    static EntityLiving dummy_entity = new EntityEnderman(null);

    public void renderItem(ItemStack is) {
        // ... copied from ServoMotor...
        // but we can't merge them because, again, item rendering code is some mad BS
        // (Well, the dummy classes could get merged.)
        // Copied from RenderBiped.renderEquippedItems

        // Pre-emptively undo transformations that the item renderer does so
        // that we don't get a stupid angle. Minecraft render code is terrible.
        boolean needRotationFix = true;
        if (is.getItem() instanceof ItemBlock) {
            Block block = Block.getBlockFromItem(is.getItem());
            if (block != null && RenderBlocks.renderItemIn3d(block.getRenderType())) {
                needRotationFix = false;
            }
        }
        GL11.glTranslatef(0, 0, 0.5F/16F);
        if (needRotationFix) {
            float scale = 10.5F / 16F;
            GL11.glScalef(scale, scale, scale);
            float wtf = (1.5F / 16F);
            float wtfY = wtf + (-0.5F / 16F);
            GL11.glTranslatef(7F / 16F + wtf, -7F / 16F + wtfY, 0F);
            GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
        }

        int itemColor = is.getItem().getColorFromItemStack(is, 0);
        float cr = (float)(itemColor >> 16 & 255) / 255.0F;
        float cg = (float)(itemColor >> 8 & 255) / 255.0F;
        float cb = (float)(itemColor & 255) / 255.0F;
        GL11.glColor4f(cr, cg, cb, 1.0F);

        this.renderManager.itemRenderer.renderItem(dummy_entity, is, 0);

        if (is.getItem().requiresMultipleRenderPasses()) {
            for (int x = 1; x < is.getItem().getRenderPasses(is.getItemDamage()); x++) {
                this.renderManager.itemRenderer.renderItem(dummy_entity, is, x);
            }
        }
    }
}
