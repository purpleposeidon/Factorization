package factorization.client.render;

import java.util.Random;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.shared.Core;

public class ChargeSparks {
    public long last_update = -1; //haxx to prevent updating while paused
    public ChargeSparks(int count) {
        sparks = new SparkInfo[count];
        for (int i = 0; i < sparks.length; i++) {
            sparks[i] = new SparkInfo();
        }
    }
    
    private static class SparkInfo {
        Vec3 src, dest;
        int life, max_life, stepTime, depth;
        double width, variance;
        int color;
        int seed;
    }
    
    private SparkInfo[] sparks;
    private int index = 0;
    
    SparkInfo take() {
        if (index == sparks.length) {
            index = 0;
        }
        return sparks[index++];
    }
    
    private static Random creationRandom = new Random();
    
    public void spark(Vec3 src, Vec3 dest, int life, int stepTime, int depth, double variance, double width, int color) {
        SparkInfo info = take();
        info.src = src;
        info.dest = dest;
        info.life = info.max_life = life;
        info.stepTime = stepTime;
        info.depth = depth;
        info.color = color;
        info.variance = variance;
        info.width = width;
        info.seed = creationRandom.nextInt();
    }
    
    public void update() {
        for (int i = 0; i < sparks.length; i++) {
            sparks[i].life--;
        }
    }
    
    double interpolate(double a, double perc) {
        //double b = 1; //Math.min(0.8, a);
        double b = a*a + 0.2;
        b = Math.min(1, b);
        return a*(1 - perc) + b*(perc);
    }
    
    double alpha_interpolate(double a, double perc) {
        perc *= perc*perc;
        double b = Math.min(0, a);
        return a*(1 - perc) + b*(perc);
    }
    
    @SideOnly(Side.CLIENT)
    public void render() {
        Tessellator tess = Tessellator.instance;
        Core.profileStart("fz_spark");
        int last_color = -1;
        tess.setColorOpaque_I(0xFFFFFF);
        boolean any = false;
        for (int i = 0; i < sparks.length; i++) {
            SparkInfo spark = sparks[i];
            if (spark.life <= 0) {
                continue;
            }
            if (!any) {
                GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                
                any = true;
            }
            
            current_spark = spark;
            end_segment = (spark.max_life - spark.life)/spark.stepTime;
            rand.setSeed(spark.seed);
            segments_drawn = 0;
            
            double f = 0xFF;
            double r = (spark.color >> 16 & 255)/f;
            double g = (spark.color >> 8 & 255)/f;
            double b = (spark.color & 255)/f;
            double alpha = 0.8;
            double depth_end = (1 << spark.depth);
            double perc = end_segment/depth_end;
            GL11.glColor4d(interpolate(r, perc), interpolate(g, perc), interpolate(b, perc), alpha_interpolate(alpha, perc));
            GL11.glLineWidth((float) spark.width);
            
            tess.startDrawing(GL11.GL_LINES);
            drawSpark(spark.src, spark.dest, spark.depth);
            tess.draw();
        }
        if (any) {
            GL11.glPopAttrib();
            GL11.glColor4f(1, 1, 1, 1);
        }
        Core.profileEnd();
    }
    
    private static Random rand = new Random();
    private static int segments_drawn, end_segment;
    private static SparkInfo current_spark;
    
    private static Vec3 work = Vec3.createVectorHelper(0, 0, 0), work2 = Vec3.createVectorHelper(0, 0, 0), delta = Vec3.createVectorHelper(0, 0, 0);
    
    void drawSpark(Vec3 start, Vec3 end, int depth) {
        if (segments_drawn > end_segment) {
            return;
        }
        
        if (depth == 0) {
            //Rather than drawing the line straight between, we render 3 lines along each axis.
            //work.xCoord = start.xCoord;
            work.yCoord = start.yCoord;
            work.zCoord = start.zCoord;
            work.xCoord = end.xCoord;
            drawLine(start, work);
            
            work2.xCoord = end.xCoord;
            work2.yCoord = end.yCoord;
            //work2.zCoord = end.zCoord;
            work2.zCoord = start.zCoord;
            drawLine(work, work2);
            drawLine(work2, end);
            
            segments_drawn++;
            return;
        }
        depth--;
        
        delta.xCoord = end.xCoord - start.xCoord;
        delta.yCoord = end.yCoord - start.yCoord;
        delta.zCoord = end.zCoord - start.zCoord;
        double d = current_spark.variance;
        d = d * depth / current_spark.depth;
        
        Vec3 mid = start.addVector(end.xCoord, end.yCoord, end.zCoord); //Don't make this static.
        mid.xCoord /= 2;
        mid.yCoord /= 2;
        mid.zCoord /= 2;
        
        mid.xCoord += delta.xCoord*d*(rand.nextDouble() - 0.5);
        mid.yCoord += delta.yCoord*d*(rand.nextDouble() - 0.5);
        mid.zCoord += delta.zCoord*d*(rand.nextDouble() - 0.5);
        
        drawSpark(start, mid, depth);
        if (segments_drawn > end_segment) {
            return;
        }
        drawSpark(mid, end, depth);
    }
    
    void drawLine(Vec3 start, Vec3 end) {
        Tessellator.instance.addVertex(start.xCoord, start.yCoord, start.zCoord);
        Tessellator.instance.addVertex(end.xCoord, end.yCoord, end.zCoord);
    }
    
}
