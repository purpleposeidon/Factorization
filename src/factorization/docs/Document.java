package factorization.docs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.Resource;
import factorization.shared.Core;

public class Document {
    String name;
    ArrayList<AbstractPage> pages;
    
    public Document(String name, ArrayList<AbstractPage> pages) {
        this.name = name;
        this.pages = pages;
    }
}
