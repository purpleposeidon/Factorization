package factorization.misc;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;

import java.io.*;
import java.nio.ByteOrder;

public class ExporterTessellatorObj extends Tessellator {
    final File filename;
    OutputStreamWriter out;
    
    public ExporterTessellatorObj(File filename) {
        this.filename = filename;
        try {
            out = new OutputStreamWriter(new FileOutputStream(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    void writeLine(String line) {
        try {
            out.write(line + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void startDrawing(int par1) {
    }
    
    @Override
    public int draw() {
        return 0;
    }
    
    public void doneDumping() {
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        out = null;
        dumpTextureMap();
    }
    
    void dumpTextureMap() {
        // :/
    }
    
    int vertexNumber = 0;
    double textureU, textureV;
    
    @Override
    public void setTextureUV(double textureU, double textureV) {
        this.textureU = textureU;
        this.textureV = -textureV; // Texture's flipped vertically
    }
    
    @Override
    public void addVertex(double x, double y, double z) {
        x += xOffset;
        y += yOffset;
        z += zOffset;
        if (hasColor) {
            float r, g, b, a;
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                r = color & 0xFF;
                g = (color >> 8) & 0xFF;
                b = (color >> 16) & 0xFF;
                a = (color >> 24) & 0xFF;
            } else {
                a = color & 0xFF;
                b = (color >> 8) & 0xFF;
                g = (color >> 16) & 0xFF;
                r = (color >> 24) & 0xFF;
            }
            r /= 0xFF;
            g /= 0xFF;
            b /= 0xFF;
            a /= 0xFF;
            writeLine("v " + x + " " + y + " " + " " + z + " " + r + " " + g + " " + b + " " + a);
        } else {
            writeLine("v " + x + " " + y + " " + " " + z);
        }
        writeLine("vt " + textureU + " " + textureV);
        vertexNumber++;
        if (vertexNumber % 4 == 0) {
            writeLine("f" + make(-3) + make(-2) + make(-1) + make(0));
        }
    }
    
    private String make(int delta) {
        int i = vertexNumber + delta;
        if (i <= 0) {
            throw new IllegalArgumentException("Invalid index");
        }
        return " " + i + "/" + i;
    }

    @Override
    public TesselatorVertexState getVertexState(float posX, float posY, float posZ) {
        if (this.rawBufferIndex <= 0) {
            return null;
        }
        return super.getVertexState(posX, posY, posZ);
    }
}
