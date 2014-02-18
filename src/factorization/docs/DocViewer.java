package factorization.docs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.Resource;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.google.common.io.Closeables;

import factorization.shared.Core;

public class DocViewer extends GuiScreen {
    //NORELEASE! Uh. Don't release a release with this user-accessible, I guess, unless the docs are slightly useful
    final String name;
    Document doc;
    Page page;
    GuiButton nextPage;
    GuiButton prevPage;
    GuiButton backButton;
    
    
    private static Deque<String> pageHistory = new ArrayDeque<String>();
    
    
    
    
    int getPageWidth(int pageNum) {
        return (width*40/100);
    }
    
    int getPageLeft(int pageNum) {
        int avail = width - getPageWidth(pageNum)*2;
        if (pageNum == 0) {
            return avail/3;
        } else {
            return getPageWidth(pageNum) + avail*2/3;
        }
    }
    
    int getPageTop(int pageNum) {
        return height*5/100;
    }
    
    int getPageHeight(int pageNum) {
        return height*90/100;
    }
    
    
    
    public static String getLatestPage() {
        if (pageHistory.isEmpty()) {
            return "index";
        }
        return pageHistory.getLast();
    }
    
    public static void addNewHistoryEntry(String name) {
        pageHistory.add(name);
    }
    
    public DocViewer(String name) {
        this.name = name;
    }
    
    @Override
    public void initGui() {
        super.initGui();
        this.doc = getDocument(name); // Rebuilds the entire document from scratch. Super-inefficient!
        if (doc == null || doc.pages.size() == 0) {
            mc.displayGuiScreen(null);
        }
        page = doc.pages.get(0);
        
        int row = getPageHeight(0);
        
        buttonList.add(prevPage = new GuiButtonNextPage(2, getPageLeft(0) - 12, row, false));
        buttonList.add(nextPage = new GuiButtonNextPage(1, getPageLeft(1) + getPageWidth(1) - 23 /* 23 is the button width */ + 12, row, true));
        buttonList.add(backButton = new GuiButton(3, (120 + 38)/2, row, 50, 20, "Back"));
    }
    
    InputStream getDocumentResource(String name) {
        try {
            Resource src = mc.getResourceManager().getResource(Core.getResource("doc/" + name + ".txt"));
            return src.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }
    
    Document getDocument(String name) {
        InputStream is = getDocumentResource(name);
        try {
            Typesetter ts = new Typesetter(mc.fontRenderer, getPageWidth(0), getPageHeight(0), readContents(is));
            return new Document(name, ts.pages);
        } finally {
            Closeables.closeQuietly(is);
        }
    }
    
    String readContents(InputStream is) {
        if (is == null) {
            return EnumChatFormatting.OBFUSCATED + "101*2*2 "
                    + EnumChatFormatting.OBFUSCATED + "Not "
                    + EnumChatFormatting.OBFUSCATED + "Found: "
                    + EnumChatFormatting.RESET + name;
        }
        try {
            StringBuilder build = new StringBuilder();
            byte[] buf = new byte[1024];
            int length;
            while ((length = is.read(buf)) != -1) {
                build.append(new String(buf, 0, length));
            }
            return build.toString();
        } catch (Throwable e) {
            String txt = e.getMessage();
            for (StackTraceElement ste : e.getStackTrace()) {
                txt += "\n\n    at " + ste.getFileName() + "(" + ste.getFileName() + ":" + ste.getLineNumber() + ")";
            }
            return EnumChatFormatting.OBFUSCATED + "5*5*5*2*2 Internal Server Error\n\nAn error was encountered while trying to execute your request.\n\n" + txt;
        }
    }
    
    Page getPage(int d) {
        if (doc == null) return null;
        if (d == 0) return page;
        int i = doc.pages.indexOf(page) + d;
        if (i < 0) return null;
        if (i >= doc.pages.size()) return null;
        return doc.pages.get(i);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        hot = false;
        drawDefaultBackground();
        
        backButton.drawButton = !pageHistory.isEmpty();
        prevPage.drawButton = doc.pages.indexOf(page) > 0;
        nextPage.drawButton = doc.pages.indexOf(page) + 2 < doc.pages.size();
        
        {
            int paddingVert = 8, paddingHoriz = 12;
            
            int x0 = getPageLeft(0) - paddingHoriz;
            int x1 = getPageLeft(1) + getPageWidth(1) + paddingHoriz;
            int y0 = getPageTop(0) - paddingVert;
            int y1 = getPageHeight(0) + paddingVert;
            
            GL11.glColor3f(0.075F, 0.075F, 0.1125F);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(x0, y0, 0);
            GL11.glVertex3f(x0, y1, 0);
            GL11.glVertex3f(x1, y1, 0);
            GL11.glVertex3f(x1, y0, 0);
            GL11.glEnd();
            
            x0 = getPageLeft(0) + getPageWidth(0) + paddingHoriz;
            x1 = getPageLeft(1) - paddingHoriz;
            
            float cs = 0.75F;
            GL11.glColor3f(0.075F*cs, 0.075F*cs, 0.1125F*cs);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(x0, y0, 0);
            GL11.glVertex3f(x0, y1, 0);
            GL11.glVertex3f(x1, y1, 0);
            GL11.glVertex3f(x1, y0, 0);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1, 1, 1);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        
        if (page != null) {
            page.draw(getPageLeft(0), getPageTop(0));
        }
        Page snd = getPage(1);
        if (snd != null) {
            snd.draw(getPageLeft(1), getPageTop(1));
        }
        
    }
    
    boolean hot = true;
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (hot) return;
        super.mouseClicked(mouseX, mouseY, button);
        
        for (int i = 0; i <= 1; i++) {
            Page p = getPage(i);
            if (p == null) continue;
            Word link = p.click(mouseX - getPageLeft(i), mouseY - getPageTop(i));
            if (link != null && link.hyperlink != null) {
                DocViewer newDoc = new DocViewer(link.hyperlink);
                addNewHistoryEntry(doc.name);
                mc.displayGuiScreen(newDoc);
                return;
            }
        }
    }
    
    @Override
    public void handleMouseInput() {
        int scroll = Mouse.getEventDWheel();
        if (scroll == 0) {
            super.handleMouseInput();
        } else if (scroll > 0) {
            actionPerformed(prevPage);
        } else if (scroll < 0) {
            actionPerformed(nextPage);
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == nextPage) {
            Page n = getPage(2);
            if (n != null) {
                page = n;
            }
        } else if (button == prevPage) {
            Page n = getPage(-2);
            if (n != null) {
                page = n;
            }
        } else if (button == backButton) {
            while (!pageHistory.isEmpty() && pageHistory.getLast().equalsIgnoreCase(name)) {
                pageHistory.removeLast();
            }
            String name = "index";
            if (pageHistory.size() > 0) {
                name = pageHistory.removeLast();
            }
            System.out.println("Back to " + name);
            DocViewer newDoc = new DocViewer(name);
            mc.displayGuiScreen(newDoc);
        }
    }
    
    @Override
    protected void keyTyped(char chr, int keySym) {
        if (keySym == Keyboard.KEY_BACK || chr == 'z') {
            actionPerformed(backButton);
        } else if (keySym == Keyboard.KEY_NEXT || chr == ' ') {
            actionPerformed(nextPage);
        } else if (keySym == Keyboard.KEY_PRIOR) {
            actionPerformed(prevPage);
        } else if (chr == 'r') {
            initGui();
        } else {
            super.keyTyped(chr, keySym);
        }
    }
    
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long heldTime) {
        super.mouseClickMove(mouseX, mouseY, button, heldTime);
    }
    
    @Override
    public void onGuiClosed() {
        /*if (doc != null && doc.name != null && !getLatestPage().equals(doc.name)) {
            addNewHistoryEntry(doc.name);	
        }*/
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
