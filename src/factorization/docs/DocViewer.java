package factorization.docs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import factorization.shared.Core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.Resource;

public class DocViewer extends GuiScreen {
    //NORELEASE! Uh. Don't release a release with this user-accessible, I guess, unless the docs are slightly useful
    final String name;
    Document doc;
    Page page;
    
    public DocViewer(String name) {
        this.name = name;
    }
    
    @Override
    public void initGui() {
        super.initGui();
        try {
            this.doc = getDocument(name);
            this.page = doc.pages.get(0);
        } catch (IOException e) {
            e.printStackTrace();
            mc.displayGuiScreen(null);
        }
    }
    
    Document getDocument(String name) throws IOException {
        final Minecraft mc = Minecraft.getMinecraft();
        Resource src = mc.getResourceManager().getResource(Core.getResource("doc/" + name));
        InputStream is = src.getInputStream();
        try {
            Typesetter ts = new Typesetter(mc.fontRenderer, width*40/100, height*80/100, readContents(is));
            return new Document(ts.pages);
        } finally {
            is.close();
        }
    }
    
    String readContents(InputStream is) throws IOException {
        StringBuilder build = new StringBuilder();
        byte[] buf = new byte[1024];
        int length;
        while ((length = is.read(buf)) != -1) {
            build.append(new String(buf, 0, length));
        }
        return build.toString();
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDefaultBackground();
        if (page == null) return;
        page.draw();
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long heldTime) {
        super.mouseClickMove(mouseX, mouseY, button, heldTime);
    }
}
