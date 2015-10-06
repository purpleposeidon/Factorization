package factorization.scrap;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

import java.util.Scanner;

@Help({"Unmaps a TESR",
        "DeregisterTesr net.minecraft.tileentity.TileEntityChest"})
public class DeregisterTesr extends AbstractMapDeregister {
    public DeregisterTesr(Scanner in) {
        super(TileEntityRendererDispatcher.instance.mapSpecialRenderers, ScannerHelper.nextClass(in));
    }
}
