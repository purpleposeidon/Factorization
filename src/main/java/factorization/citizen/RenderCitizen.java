package factorization.citizen;

import factorization.api.Quaternion;
import factorization.fzds.interfaces.Interpolation;
import factorization.shared.FzModel;
import factorization.net.NetworkFactorization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderCitizen extends RenderEntity {
    private EntityLiving dummy_entity = new EntityEnderman(null);

    public RenderCitizen(RenderManager renderManagerIn) {
        super(renderManagerIn);
    }

    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partial) {
        EntityCitizen citizen = (EntityCitizen) ent;
        if (!citizen.visible) return;


        Minecraft.getMinecraft().getTextureManager().bindTexture(getEntityTexture(ent));
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        float t = (partial + citizen.spinning_ticks) / EntityCitizen.TICKS_PER_SPIN;
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        t = (float) Interpolation.SMOOTHER.scale(t);
        Quaternion mid;
        if (citizen.rotation_start == citizen.rotation_target) {
            mid = citizen.rotation_start;
        } else {
            mid = citizen.rotation_start.slerp(citizen.rotation_target, t);
        }
        mid.glRotate();

        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glRotatef(-90, 0, 0, 1);
        model.draw();
        GL11.glPopMatrix();
        if (citizen.held != null && citizen.held.stackSize > 0) {
            float s = 0.5F;
            GL11.glScalef(s, s, s);
            GL11.glRotatef(90, 1, 0, 0);
            GL11.glRotatef(90, 0, 1, 0);
            float now = ent.worldObj.getTotalWorldTime() + partial;
            float angle = 40 + (float) Interpolation.SMOOTH.scale(Math.abs(Math.sin(now / 20))) * 5;
            GL11.glRotatef(angle, 0, 1, 0);
            GL11.glTranslatef(-1.25F, -3.25F / 16F, 0);
            EntityItem entityitem = new EntityItem(null, 0, 0, 0, NetworkFactorization.EMPTY_ITEMSTACK.copy());
            entityitem.setEntityItemStack(citizen.held);
            entityitem.hoverStart = 0.0F;
            GameSettings gs = Minecraft.getMinecraft().gameSettings;
            boolean fancy = gs.fancyGraphics;
            gs.fancyGraphics = true;
            Minecraft.getMinecraft().getRenderManager().renderEntityWithPosYaw(entityitem, 1, 0, 0, 0, 0);
            gs.fancyGraphics = fancy;
        }
        GL11.glPopMatrix();
    }

    ResourceLocation skin = new ResourceLocation("factorization", "textures/entity/citizen.png");
    static FzModel model = new FzModel("citizen");

    @Override
    protected ResourceLocation getEntityTexture(Entity ent) {
        return skin;
    }
}
