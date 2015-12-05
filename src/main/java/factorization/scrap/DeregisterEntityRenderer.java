package factorization.scrap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;

import java.util.Scanner;

public class DeregisterEntityRenderer extends AbstractMapDeregister {
    public DeregisterEntityRenderer(Scanner in) {
        super(Minecraft.getMinecraft().getRenderManager().entityRenderMap, ScannerHelper.nextClass(in));
    }
}
