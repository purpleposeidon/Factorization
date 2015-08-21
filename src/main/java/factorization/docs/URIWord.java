package factorization.docs;

import factorization.shared.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class URIWord extends TextWord implements GuiYesNoCallback {
    final String uriString;

    public URIWord(String text, String uri) {
        super(text, "?");
        this.uriString = uri;
    }

    private static final List<String> safeProtocolSchemes = Arrays.asList("http", "https");
    private GuiScreen origGui = null;
    private URI uri;

    @Override
    public boolean onClick() {
        try {
            uri = new URI(uriString);

            final String scheme = uri.getScheme().toLowerCase();
            if (!safeProtocolSchemes.contains(scheme)) {
                uri = null;
                throw new URISyntaxException(uriString, "Unsupported protocol: " + scheme);
            }

            final Minecraft mc = Minecraft.getMinecraft();
            origGui = mc.currentScreen;
            if (mc.gameSettings.chatLinksPrompt) {
                mc.displayGuiScreen(new GuiConfirmOpenLink(this, uriString, 0, false));
            } else {
                visitLink(uri);
            }
        } catch (URISyntaxException urisyntaxexception) {
            Core.logWarning("Can't open url for " + uriString, urisyntaxexception);
        }
        return true;
    }

    @Override
    public void confirmClicked(boolean doOpen, int someWeirdNumber) {
        if (someWeirdNumber != 0) return;
        if (doOpen) {
            visitLink(uri);
        }
        uri = null;
        Minecraft.getMinecraft().displayGuiScreen(origGui);
    }

    void visitLink(URI uri) {
        try {
            Class AwtDesktop = Class.forName("java.awt.Desktop");
            Object desktop = AwtDesktop.getMethod("getDesktop").invoke(null);
            AwtDesktop.getMethod("browse", URI.class).invoke(desktop, uri);
        } catch (Throwable throwable) {
            Core.logWarning("Couldn't open link", throwable);
        }
    }
}
