package factorization.common.servo;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.Core;

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
        case WEST: break;
        case EAST: GL11.glRotatef(180, 0, 1, 0); break;
        case NORTH: GL11.glRotatef(-90, 0, 1, 0); break;
        case SOUTH: GL11.glRotatef(90, 0, 1, 0); break;
        case UP: GL11.glRotatef(-90, 0, 0, 1); break;
        case DOWN: GL11.glRotatef(90, 0, 0, 1); break;
        }
    }
    
    static Vec3 quat_vector = Vec3.createVectorHelper(0, 0, 0);
    static Quaternion start = new Quaternion(), end = new Quaternion();
    
    float interp(double a, double b, double part) {
        double d = a - b;
        float r =  (float) (b + d*part);
        return r;
    }
    
    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partial) {
        //super.doRender(ent, x, y, z, yaw, partial);
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
        if (loaded_model == false) {
            loadSprocketModel();
            loaded_model = true;
        }
        loadTexture(Core.model_dir + "sprocket/servo_uv.png");
        ServoMotor motor = (ServoMotor) ent;
        motor.interpolatePosition((float) Math.pow(motor.pos_progress, 2));
        //final FzOrientation orientation = FzOrientation.FACE_WEST_POINT_SOUTH; //motor.orientation;
        //final FzOrientation prevOrientation = orientation; //FzOrientation.FACE_DOWN_POINT_EAST;//motor.prevOrientation;
        final FzOrientation orientation = motor.orientation;
        final FzOrientation prevOrientation = motor.prevOrientation;
        
        //NORELEASE: debug facing
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
        
        //Servo facing
        float ro = interp(motor.servo_reorient, motor.prev_servo_reorient, partial);
        ro = (float) Math.min(1, ro*1.1);
        ro = (float) Math.cbrt(ro);
        Quaternion qt;
        if (prevOrientation == orientation) {
            qt = Quaternion.fromOrientation(orientation);
        } else {
            Quaternion q0 = new Quaternion(Quaternion.fromOrientation(prevOrientation));
            Quaternion q1 = new Quaternion(Quaternion.fromOrientation(orientation));
            q0.incrLerp(q1, ro);
            qt = q0;
        }
        qt.glRotate();
        GL11.glRotatef(90, 0, 0, 1);
        
        GL11.glColor3f(1, 0, 1);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(1, 0, 0);
        GL11.glVertex3d(1, 1, 0);
        GL11.glVertex3d(0, 0, 0);
        GL11.glEnd();
        GL11.glColor3f(1, 1, 1);
        
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        
        //Scale
        float s = 0.5F;
        GL11.glScalef(s, s, s);
        
        //Sprocket rotation
        double rail_width = TileEntityServoRail.width;
        double radius = 0.56 /* from sprocket center to the outer edge of the ring (excluding the teeth) */ + 0.06305 /* half the width of the teeth */; //0.25 + 1.5/48.0;
        double constant = Math.PI*2*(radius);
        double dr = motor.sprocket_rotation - motor.prev_sprocket_rotation;
        double partial_rotation = motor.prev_sprocket_rotation + dr*partial;
        double angle = constant*partial_rotation;
        
        radius = 0.25 + 1.5/48.0;
        //radius /= 2;
        
        float rd = (float) (radius + rail_width);
        //Sprocket rendering. (You wouldn't have been able to tell by reading the code.)
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, rd);
            GL11.glRotatef((float)Math.toDegrees(angle), 0, 1, 0);
            renderSprocket();
            GL11.glPopMatrix();
        }
        {
            GL11.glTranslatef(0, 0, -rd);
            //GL11.glTranslatef(0, 0, (float)(2 - radius));
            //GL11.glTranslatef(0, 0, (float)(2 - radius));
            //GL11.glTranslatef(0, 0, 2*(float)(radius*2 + TileEntityServoRail.width/2));
            GL11.glRotatef((float)Math.toDegrees(-angle) + 360F/9F, 0, 1, 0);
            renderSprocket();
        }
        GL11.glPopMatrix();
        motor.interpolatePosition(motor.pos_progress);
    }

}
