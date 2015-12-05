package factorization.servo.stepper;

import factorization.api.FzColor;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.fzds.DeltaChunk;
import factorization.fzds.Hammer;
import factorization.fzds.HammerEnabled;
import factorization.servo.BlockRenderServoRail;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.util.NumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class RenderStepperEngine extends RenderEntity {
    ObjectModel sprocket = new ObjectModel(Core.getResource("models/servo/sprocket.obj"));
    ObjectModel chasis = new ObjectModel(Core.getResource("models/servo/stepper.obj"));

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

    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partial) {
        Core.profileStartRender("servo");
        //Ugh, there's some state that changes when mousing over an item in the inventory...
        MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
        if (HammerEnabled.ENABLED && DeltaChunk.getClientShadowWorld() == ent.worldObj) {
            mop = Hammer.proxy.getShadowHit();
        }
        boolean highlighted = mop != null && mop.entityHit == ent;
        StepperEngine motor = (StepperEngine) ent;

        GL11.glPushMatrix();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
        GL11.glPushMatrix();

        motor.motionHandler.interpolatePosition((float) Math.pow(motor.motionHandler.pos_progress, 2));
        float reorientInterpolation = interp(motor.motionHandler.servo_reorient, motor.motionHandler.prev_servo_reorient, partial);
        orientMotor(motor, partial, reorientInterpolation);

        renderMainModel(motor, partial, reorientInterpolation, false);

        if (highlighted) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            float gray = 0.65F;
            GL11.glColor4f(gray, gray, gray, 0.8F);
            GL11.glLineWidth(1.5F);
            float d = 1F/2F, h = 0.25F;
            AxisAlignedBB ab = new AxisAlignedBB(-d, -h, -d, d, h, d);
            drawOutlinedBoundingBox(ab);
            ab.offset(ab.minX, ab.minY, ab.minZ);
            GL11.glPopAttrib();
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            EntityPlayer player = Core.proxy.getClientPlayer();
            if (player != null) {
                for (int i = 0; i < 9; i++) {
                    ItemStack is = player.inventory.getStackInSlot(i);
                    if (is == null) continue;
                    if (is.getItem() == Core.registry.logicMatrixProgrammer) {
                        break;
                    }
                }
            }
        }

        GL11.glPopMatrix();
        GL11.glPopMatrix();
        motor.motionHandler.interpolatePosition(motor.motionHandler.pos_progress);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        Core.profileEndRender();
    }

    void drawOutlinedBoundingBox(AxisAlignedBB par1AxisAlignedBB) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(3);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
        tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
        tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
        tessellator.draw();
        tessellator.startDrawing(3);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
        tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
        tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
        tessellator.draw();
        tessellator.startDrawing(1);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
        tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.minZ);
        tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.minZ);
        tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
        tessellator.addVertex(par1AxisAlignedBB.maxX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.minY, par1AxisAlignedBB.maxZ);
        tessellator.addVertex(par1AxisAlignedBB.minX, par1AxisAlignedBB.maxY, par1AxisAlignedBB.maxZ);
        tessellator.draw();
    }

    void orientMotor(StepperEngine motor, float partial, float reorientInterpolation) {
        final FzOrientation orientation = motor.motionHandler.orientation;
        FzOrientation prevOrientation = motor.motionHandler.prevOrientation;
        if (prevOrientation == FzOrientation.UNKNOWN) {
            prevOrientation = orientation;
        }

        if (debug_servo_orientation) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glLineWidth(4);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            FzOrientation o = orientation;
            GL11.glColor3f(1, 0, 0);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(o.facing.getDirectionVec().getX(), o.facing.getDirectionVec().getY(), o.facing.getDirectionVec().getZ());
            GL11.glVertex3d(o.facing.getDirectionVec().getX() + o.top.getDirectionVec().getX(), o.facing.getDirectionVec().getY() + o.top.getDirectionVec().getY(), o.facing.getDirectionVec().getZ() + o.top.getDirectionVec().getZ());
            GL11.glEnd();
            GL11.glLineWidth(2);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            o = prevOrientation;
            GL11.glColor3f(0, 0, 1);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(o.facing.getDirectionVec().getX(), o.facing.getDirectionVec().getY(), o.facing.getDirectionVec().getZ());
            GL11.glVertex3d(o.facing.getDirectionVec().getX() + o.top.getDirectionVec().getX(), o.facing.getDirectionVec().getY() + o.top.getDirectionVec().getY(), o.facing.getDirectionVec().getZ() + o.top.getDirectionVec().getZ());
            GL11.glEnd();
        }

        // Servo facing
        Quaternion qt;
        if (prevOrientation == orientation) {
            qt = Quaternion.fromOrientation(orientation);
        } else {
            q0.update(Quaternion.fromOrientation(prevOrientation));
            q1.update(Quaternion.fromOrientation(orientation));
            if (q0.dotProduct(q1) < 0) {
                q0.incrScale(-1);
            }
            q0.incrLerp(q1, reorientInterpolation);
            qt = q0;
        }
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
    }


    @Override
    protected ResourceLocation getEntityTexture(Entity ent) {
        return Core.blockAtlas;
    }

    void renderMainModel(StepperEngine motor, float partial, double ro, boolean hilighting) {
        GL11.glPushMatrix();
        bindTexture(Core.blockAtlas);
        chasis.render(BlockIcons.servo$model$stepper);

        FzColor c = motor.motionHandler.color;
        renderServoColor(c);
        GL11.glColor3f(c.getRed(), c.getGreen(), c.getBlue());

        // Determine the sprocket location & rotation
        double radius = 0.5;
        double constant = Math.PI * 2 * (radius);
        double partial_rotation = NumUtil.interp((float) motor.motionHandler.prev_sprocket_rotation, (float) motor.motionHandler.sprocket_rotation, partial);
        final double angle = constant * partial_rotation;

        float rd = (float) radius;
        if (motor.motionHandler.orientation != motor.motionHandler.prevOrientation && motor.motionHandler.prevOrientation != FzOrientation.UNKNOWN) {
            // This could use some work: only stretch if the new direction is parallel to the old gear direction.
            double stretch_interp = ro * 2;
            if (stretch_interp < 1) {
                if (stretch_interp > 0.5) {
                    stretch_interp = 1 - stretch_interp;
                }
                rd += stretch_interp / 8;
            }
        }
        // Render them
        float o = 8F/16F;
        float height_d = 2F/16F;
        GL11.glRotatef(180, 1, 0, 0);
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(o, height_d, rd);
            GL11.glRotatef((float) Math.toDegrees(angle), 0, 1, 0);
            sprocket.render(BlockIcons.servo$model$sprocket);
            GL11.glPopMatrix();
        }
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(o, height_d, -rd);
            GL11.glRotatef((float) Math.toDegrees(-angle) + 360F / 9F, 0, 1, 0);
            sprocket.render(BlockIcons.servo$model$sprocket);
            GL11.glPopMatrix();
        }

        GL11.glColor3f(1, 1, 1);
        GL11.glPopMatrix();
    }

    void renderServoColor(FzColor color) {
        if (color == FzColor.NO_COLOR) return;
        IIcon colorIcon = BlockRenderServoRail.coloredRails[color.toVanillaColorIndex()];
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.setBlockBoundsOffset(0, 0, 0);
        block.useTexture(null);
        block.setTexture(1 /* up */, colorIcon);
        block.beginWithMirroredUVs();
        GL11.glPushMatrix();
        float d = -0.5F;
        GL11.glTranslatef(d, d - 3F/8F + 0.0001F, d);
        {
            // We need to get 14/16ths transformed for 10/16ths.
            float b = 14F/16F;
            GL11.glScalef(1/b, 1, 1/b);
            float s = 10F/16F;
            GL11.glScalef(s, 1, s);
            float t = 3.2F/16F;
            GL11.glTranslatef(t, 0, t);
        }
        Tessellator.instance.startDrawingQuads();
        block.renderForTileEntity();
        //GL11.glDisable(GL11.GL_LIGHTING);
        Tessellator.instance.draw();
        //GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }
}
