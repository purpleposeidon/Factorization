package factorization.scrap;

import net.minecraft.client.renderer.entity.RenderManager;

import java.util.Scanner;

public class DeregisterEntityRenderer extends AbstractMapDeregister {
    public DeregisterEntityRenderer(Scanner in) {
        super(RenderManager.instance.entityRenderMap, ScannerHelper.nextClass(in));
    }
}
