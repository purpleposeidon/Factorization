package factorization.misc;

import java.io.*;
import java.nio.ByteOrder;

import factorization.shared.NORELEASE;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;
import org.lwjgl.opengl.GL11;

public class ExporterTessellatorPly extends Tessellator {
    final File filename, tmp_vert, tmp_face;
    OutputStreamWriter vertices;
    OutputStreamWriter faces;

    public ExporterTessellatorPly(File filename) {
        this.filename = filename;
        this.tmp_vert = new File(this.filename + ".tmp_vert");
        this.tmp_face = new File(this.filename + ".tmp_face");
        try {
            vertices = new OutputStreamWriter(new FileOutputStream(tmp_vert));
            faces = new OutputStreamWriter(new FileOutputStream(tmp_face));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    void finish() throws IOException {
        vertices.close();
        faces.close();

        final FileOutputStream fos = new FileOutputStream(filename);
        OutputStreamWriter out = new OutputStreamWriter(fos);
        out.write("ply\n");
        out.write("format ascii 1.0\n");
        out.write("comment Exported by Factorization\n");
        out.write("element vertex " + vertexNumber + "\n");
        out.write("property float x\n");
        out.write("property float y\n");
        out.write("property float z\n");
        out.write("property float s\n");
        out.write("property float t\n");
        out.write("property uchar red\n");
        out.write("property uchar green\n");
        out.write("property uchar blue\n");
        out.write("property uchar alpha\n");
        out.write("element face " + vertexNumber / 4 + "\n");
        out.write("property list uchar uint vertex_indices\n");
        out.write("end_header\n");
        out.flush();

        append(fos, tmp_vert);
        append(fos, tmp_face);
        out.close();

        //noinspection ResultOfMethodCallIgnored
        tmp_vert.delete();
        //noinspection ResultOfMethodCallIgnored
        tmp_face.delete();
    }

    private void append(FileOutputStream out, File filename) throws IOException {
        FileInputStream in = new FileInputStream(filename);
        byte buff[] = new byte[1024];
        while (true) {
            int got = in.read(buff);
            if (got == -1) {
                in.close();
                return;
            }
            out.write(buff, 0, got);
        }
    }

    void vert(String line) {
        try {
            vertices.write(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void face(String line) {
        try {
            faces.write(line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startDrawing(int shapeKind) {
        if (shapeKind != GL11.GL_QUADS) {
            throw new RuntimeException("This tessellator can only handle GL_QUADS");
        }
        r = g = b = a = 0xFF;
    }

    @Override
    public int draw() {
        return 0;
    }

    public void doneDumping() {
        try {
            finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    double textureU, textureV;

    @Override
    public void setTextureUV(double textureU, double textureV) {
        this.textureU = textureU;
        this.textureV = textureV;
    }

    int r, g, b, a;
    @Override
    public void setColorRGBA(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }


    int vertexNumber = 0;

    @Override
    public void addVertex(double x, double y, double z) {
        /*x += xOffset;
        y += yOffset;
        z += zOffset;*/
        // y & z are swapped, UVs are flipped
        vert(x + " " + z + " " + y + " " + textureU + " " + (1 - textureV) + " " + r + " " + g + " " + b + " " + a + "\n");
        if (vertexNumber % 4 == 0) {
            face("4" + make(-1) + make(-2) + make(-3) + make(-4) + "\n");
        }
        vertexNumber++;
    }

    private String make(int delta) {
        return " " + (vertexNumber + delta);
    }

    @Override
    public TesselatorVertexState getVertexState(float posX, float posY, float posZ) {
        if (this.rawBufferIndex <= 0) {
            NORELEASE.breakpoint();
            return null; // !!??
        }
        return super.getVertexState(posX, posY, posZ);
    }
}
