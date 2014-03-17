package factorization.docs;

import java.util.ArrayList;

public class Document {
    String name;
    ArrayList<AbstractPage> pages;
    
    public Document(String name, ArrayList<AbstractPage> pages) {
        this.name = name;
        this.pages = pages;
    }
}
