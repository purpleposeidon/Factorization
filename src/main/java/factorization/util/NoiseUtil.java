package factorization.util;

import factorization.shared.Core;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class NoiseUtil {
    public static class Dumper {
        final int width, height;
        final BufferedImage img;

        public Dumper(Class<?> devtimeAssertionClass, int width, int height) {
            width += 8;
            height += 8;
            this.width = width;
            this.height = height;
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = img.createGraphics();
            graphics.setColor(Color.green);
            graphics.fillRect(0, 0, width + 2, height + 2);
            graphics.dispose();
        }

        public void give(int x, int y, double sample) {
            x += 4;
            y += 4;
            if (x < 0 || x >= width) return;
            if (y < 0 || y >= height) return;
            int color = 0;
            int i = (int)(Math.abs(sample) * 255);
            if (sample < 0) {
                color = i << 16;
            } else {
                color = (i << 16) | (i << 8) | i;
            }
            img.setRGB(x, y, color);
        }

        public void finish(String name) {
            File output = new File("/tmp/" + name + ".png");
            Core.logInfo("Writing noise to: " + output);
            try {
                ImageIO.write(img, "png", output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
