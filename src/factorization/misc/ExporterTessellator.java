package factorization.misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import net.minecraft.client.renderer.Tessellator;

public class ExporterTessellator extends Tessellator {
    final File filename;
    OutputStreamWriter out;
    
    public ExporterTessellator(File filename) {
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
    /*
    public void writeImage(String par1Str)
    {
        BufferedImage bufferedimage = new BufferedImage(this.width, this.height, 2);
        ByteBuffer bytebuffer = this.getTextureData();
        byte[] abyte = new byte[this.width * this.height * 4]; 
        bytebuffer.position(0);
        bytebuffer.get(abyte);

        for (int i = 0; i < this.width; ++i)
        {   
            for (int j = 0; j < this.height; ++j)
            {   
                int k = j * this.width * 4 + i * 4;
                byte b0 = 0;
                int l = b0 | (abyte[k + 2] & 255) << 0;
                l |= (abyte[k + 1] & 255) << 8;
                l |= (abyte[k + 0] & 255) << 16; 
                l |= (abyte[k + 3] & 255) << 24; 
                bufferedimage.setRGB(i, j, l); 
            }   
        }   

        this.textureData.position(this.width * this.height * 4);

        try
        {
            ImageIO.write(bufferedimage, "png", new File(Minecraft.getMinecraftDir(), par1Str));
        }
        catch (IOException ioexception)
        {
            ioexception.printStackTrace();
        }
    }
    */
    
    int vertexNumber = 0;
    double textureU, textureV;
    
    @Override
    public void setTextureUV(double textureU, double textureV) {
        this.textureU = textureU;
        this.textureV = textureV;
    }
    
    @Override
    public void addVertex(double x, double y, double z) {
        writeLine("v " + x + " " + y + " " + " " + z);
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
}
