package factorization.common.servo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;

public class RenderServoMotor extends RenderEntity {
    static int sprocket_display_list = -1;
    boolean loaded_model = false;

    void loadSprocketModel() {
        IModelCustom sprocket = AdvancedModelLoader.loadModel(Core.model_dir + "sprocket/sprocket.obj");
        sprocket_display_list = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(sprocket_display_list, GL11.GL_COMPILE);
        sprocket.renderAll();
        GL11.glEndList();
    }

    void renderSprocket() {
        GL11.glCallList(sprocket_display_list);
    }

    ForgeDirection getPerpendicular(ForgeDirection a, ForgeDirection b) {
        return a.getRotation(b);
    }

    void rotateForDirection(ForgeDirection dir) {
        switch (dir) {
        case WEST:
            break;
        case EAST:
            GL11.glRotatef(180, 0, 1, 0);
            break;
        case NORTH:
            GL11.glRotatef(-90, 0, 1, 0);
            break;
        case SOUTH:
            GL11.glRotatef(90, 0, 1, 0);
            break;
        case UP:
            GL11.glRotatef(-90, 0, 0, 1);
            break;
        case DOWN:
            GL11.glRotatef(90, 0, 0, 1);
            break;
        }
    }

    static Vec3 quat_vector = Vec3.createVectorHelper(0, 0, 0);
    static Quaternion start = new Quaternion(), end = new Quaternion();

    float interp(double a, double b, double part) {
        double d = a - b;
        float r = (float) (b + d * part);
        double v;
        // h(x,k) = (sin(x∙pi∙4.5)^2)∙x
        // v = Math.pow(Math.sin(r*Math.PI*4.5), 2)*r;
        v = Math.min(1, r * r * 4);
        return (float) v;
    }

    private Quaternion q0 = new Quaternion(), q1 = new Quaternion();
    private static boolean debug_servo_orientation = false;

    public void doRender(Entity ent, double x, double y, double z, float yaw, float partial) {
        MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
        boolean highlighted = mop != null && mop.entityHit == ent;
        ServoMotor motor = (ServoMotor) ent;

        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);

        motor.interpolatePosition((float) Math.pow(motor.pos_progress, 2));

        final FzOrientation orientation = motor.orientation;
        final FzOrientation prevOrientation = motor.prevOrientation;

        if (debug_servo_orientation) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glLineWidth(4);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            FzOrientation o = orientation;
            GL11.glColor3f(1, 0, 0);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(o.facing.offsetX, o.facing.offsetY, o.facing.offsetZ);
            GL11.glVertex3d(o.facing.offsetX + o.top.offsetX, o.facing.offsetY + o.top.offsetY, o.facing.offsetZ + o.top.offsetZ);
            GL11.glEnd();
            GL11.glLineWidth(2);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            o = prevOrientation;
            GL11.glColor3f(0, 0, 1);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(o.facing.offsetX, o.facing.offsetY, o.facing.offsetZ);
            GL11.glVertex3d(o.facing.offsetX + o.top.offsetX, o.facing.offsetY + o.top.offsetY, o.facing.offsetZ + o.top.offsetZ);
            GL11.glEnd();
        }

        // Servo facing
        float ro = interp(motor.servo_reorient, motor.prev_servo_reorient, partial);
        // ro = (float) Math.min(1, ro*ro*4);
        Quaternion qt;
        if (prevOrientation == orientation) {
            qt = Quaternion.fromOrientation(orientation);
        } else {
            q0.update(Quaternion.fromOrientation(prevOrientation));
            q1.update(Quaternion.fromOrientation(orientation));
            if (q0.dotProduct(q1) < 0) {
                q0.incrScale(-1);
            }
            q0.incrLerp(q1, ro);
            qt = q0;
        }
        // Minecraft.getMinecraft().thePlayer.addChatMessage(prevOrientation +
        // "-->" + orientation + ": " + ro); Be sure to tag this.
        qt.glRotate();
        GL11.glRotatef(90, 0, 0, 1);

        if (debug_servo_orientation) {
            GL11.glColor3f(1, 0, 1);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(1, 0, 0);
            GL11.glVertex3d(1, 1, 0);
            GL11.glVertex3d(0, 0, 0);
            GL11.glEnd();
            GL11.glColor3f(1, 1, 1);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LIGHTING);
        }

        // Scale
        float s = 0.5F;
        GL11.glScalef(s, s, s);

        renderMainModel(motor, partial, ro);
        if (highlighted) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            boolean dso = debug_servo_orientation;
            debug_servo_orientation = false;
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.25F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            renderMainModel(motor, partial, ro);
            debug_servo_orientation = dso;
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glDisable(GL11.GL_BLEND);
            EntityPlayer player = Core.proxy.getClientPlayer();
            if (player != null) {
                ItemStack is = player.getHeldItem();
                final ItemStack helmet = player.getCurrentArmor(3);
                if (is != null && is.getItem() == Core.registry.logicMatrixProgrammer || FactorizationUtil.oreDictionarySimilar("visionInducingEyewear", helmet)) {
                    renderInventory(motor, partial);
                }
            }
        }
        GL11.glScalef(1 / s, 1 / s, 1 / s);
        renderItem(motor, motor.getActuator(), partial);
        motor.interpolatePosition(motor.pos_progress);
        GL11.glPopMatrix();
    }

    RenderItem renderItem = new RenderItem();

    void renderInventory(ServoMotor motor, float partial) {
        //float s = -1; ///16F;
        float s = 1/2F;
        ItemStack actuator = motor.getActuator();
        FzInv inv = motor.getInv();
        final Minecraft mc = Minecraft.getMinecraft();
        long range = 9*20;
        double d = motor.worldObj.getWorldTime() + partial;
        float now = (float) ((d % range)/(double)range);
        //double warp = Math.cos(Math.toRadians(360*now/20)) + 2;
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            count += inv.get(i) == null ? 0 : 1;
        }
        short short_count = (short) Math.min(count, 6); //7, 8, and 9 have awkward harmonics
        int c = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack is = inv.get(i);
            if (/*is == actuator || */ is == null) {
                continue;
            }
            c++;
            GL11.glPushMatrix();
            float theta = 360/count*c + 360*now;
            GL11.glRotatef(theta, 0, 1, 0);
            GL11.glTranslatef(0.75F + (float)Math.cos(Math.toRadians(theta*short_count))/16, 0.5F, 0);
            GL11.glScalef(s, s, s);
            
            try {
                renderItem(motor, is, partial);
            } catch (Exception e) {
                System.err.println("Error rendering item: " + is);
                e.printStackTrace();
                inv.set(i, null);
            }
            /*RenderEngine re = mc.renderEngine;
            FontRenderer fr = mc.fontRenderer;
            if (!ForgeHooksClient.renderInventoryItem(renderBlocks, re, is, true, 0, 0, 0)) {
                renderItem.renderItemIntoGUI(fr, re, is, 0, 0);
            }*/
            GL11.glPopMatrix();
        }
    }

    void renderMainModel(ServoMotor motor, float partial, double ro) {
        // TODO: Put our textures into ItemIcons
        GL11.glPushMatrix();
        // super.doRender(ent, x, y, z, yaw, partial);
        if (loaded_model == false) {
            loadSprocketModel();
            loaded_model = true;
        }
        loadTexture(Core.model_dir + "sprocket/servo_uv.png");

        // Sprocket rotation
        double rail_width = TileEntityServoRail.width;
        double radius = 0.56 /*
                             * from sprocket center to the outer edge of the
                             * ring (excluding the teeth)
                             */+ 0.06305 /* half the width of the teeth */; // 0.25
                                                                            // +
                                                                            // 1.5/48.0;
        double constant = Math.PI * 2 * (radius);
        double dr = motor.sprocket_rotation - motor.prev_sprocket_rotation;
        double partial_rotation = motor.prev_sprocket_rotation + dr * partial;
        double angle = constant * partial_rotation;

        radius = 0.25 + 1.5 / 48.0;
        // radius /= 2;

        float rd = (float) (radius + rail_width);
        if (motor.orientation != motor.prevOrientation) {
            double stretch_interp = ro * 2;
            if (stretch_interp < 1) {
                if (stretch_interp > 0.5) {
                    stretch_interp = 1 - stretch_interp;
                }
                rd += stretch_interp / 8;
            }
        }
        // Sprocket rendering. (You wouldn't have been able to tell by reading
        // the code.)
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, rd);
            GL11.glRotatef((float) Math.toDegrees(angle), 0, 1, 0);
            renderSprocket();
            GL11.glPopMatrix();
        }
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, -rd);
            // GL11.glTranslatef(0, 0, (float)(2 - radius));
            // GL11.glTranslatef(0, 0, (float)(2 - radius));
            // GL11.glTranslatef(0, 0, 2*(float)(radius*2 +
            // TileEntityServoRail.width/2));
            GL11.glRotatef((float) Math.toDegrees(-angle) + 360F / 9F, 0, 1, 0);
            renderSprocket();
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }

    static ItemStack equiped_item = null;

    static EntityLiving item_holder = new EntityLiving(null) {
        @Override
        public int getMaxHealth() {
            return 0;
        }

        public ItemStack getHeldItem() {
            return equiped_item;
        }
    };

    private static class HolderRenderer extends RenderBiped {

        public HolderRenderer(ModelBiped model, float someFloat) {
            super(model, someFloat);
        }

        public void renderItem(float partial) {
            renderEquippedItems(item_holder, partial);
            // renderModel(item_holder, 0, 0, 0, 0, 0, 0);
        }
    }

    static HolderRenderer holder_render = new HolderRenderer(new ModelZombie(), 1);
    static EntityLiving dummy_entity = new EntityEnderman(null);

    void renderItem(ServoMotor motor, ItemStack is, float partial) {
        equiped_item = is;
        if (equiped_item == null) {
            return;
        }
        dummy_entity.worldObj = motor.worldObj;
        holder_render.setRenderManager(renderManager);
        // holder_render.renderItem(partial);
        do_renderItem(equiped_item);
    }

    public void do_renderItem(ItemStack itemstack) {
        // Yoinked from RenderBiped.renderEquippedItems
        GL11.glPushMatrix();
        GL11.glRotatef(180 - 45, 0, 1, 0);
        float s = 1F / 2F;
        GL11.glScalef(s, s, s);

        // GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);
        this.renderManager.itemRenderer.renderItem(dummy_entity, itemstack, 0);

        if (itemstack.getItem().requiresMultipleRenderPasses()) {
            for (int x = 1; x < itemstack.getItem().getRenderPasses(itemstack.getItemDamage()); x++) {
                this.renderManager.itemRenderer.renderItem(dummy_entity, itemstack, x);
            }
        }

        GL11.glPopMatrix();
    }

    protected void func_82422_c() {
        GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
    }
}
