package factorization.citizen;

import factorization.shared.ObjectModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderCitizen extends RenderEntity {
    private EntityLiving dummy_entity = new EntityEnderman(null);

    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partial) {
        EntityCitizen citizen = (EntityCitizen) ent;
        if (!citizen.visible) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(getEntityTexture(ent));
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        if (citizen.held != null && citizen.held.stackSize > 0) {
            this.renderManager.itemRenderer.renderItem(dummy_entity, citizen.held, 0);
        }

        GL11.glRotatef(90, 1, 0, 0);
        GL11.glRotatef(-90, 0, 0, 1);
        model.render();
        GL11.glPopMatrix();
    }

    ResourceLocation skin = new ResourceLocation("factorization", "textures/entity/citizen.png");
    ObjectModel model = new ObjectModel(new ResourceLocation("factorization", "models/citizen.obj"));

    @Override
    protected ResourceLocation getEntityTexture(Entity ent) {
        return skin;
    }
}
