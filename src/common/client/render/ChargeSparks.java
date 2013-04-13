package factorization.client.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.Vec3;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ChargeSparks {
    private static class SparkInfo {
        Vec3 src, dest;
        int life, stepTime, steps;
        float width;
        int color;
    }
    
    private SparkInfo[] sparks;
    int index = 0;
    
    public ChargeSparks(int count) {
        sparks = new SparkInfo[count];
        for (int i = 0; i < sparks.length; i++) {
            sparks[i] = new SparkInfo();
        }
    }
    
    SparkInfo take() {
        if (index == sparks.length) {
            index = 0;
        }
        return sparks[index++];
    }
    
    public void spark(Vec3 src, Vec3 dest, int life, int stepTime, int steps, float width, int color) {
        SparkInfo info = take();
        info.src = src;
        info.dest = dest;
        info.life = life;
        info.stepTime = stepTime;
        info.steps = steps;
        info.color = color;
        info.width = width;
    }
    
    public void update() {
        for (int i = 0; i < sparks.length; i++) {
            sparks[i].life--;
        }
    }
    
    
    @SideOnly(Side.CLIENT)
    public void render() {
        boolean any = false;
        int last_color = -1;
        //For calculating normals:
        //GL11.glRotatef(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        //GL11.glRotatef(this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        for (int i = 0; i < sparks.length; i++) {
            SparkInfo spark = sparks[i];
            if (spark.life <= 0) {
                continue;
            }
            if (!any) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }
            any = true;
            if (spark.color != last_color) {
                last_color = spark.color;
                GL11.glColor3b((byte)(last_color & 0xFF0000), (byte)(last_color & 0x00FF00), (byte)(last_color & 0x0000FF));
            }
            for (int step = 0; step < spark.steps; step++) {
                
            }
        }
        if (any) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1, 1, 1);
        } //GL_QUAD_STRIP
    }
}
