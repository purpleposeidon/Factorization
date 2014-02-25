package factorization.docs;

public abstract class AbstractPage {
    String title = null;
    abstract void draw(DocViewer doc, int ox, int oy);
    void closed() {}
    void mouseDragStart() {}
    void mouseDrag(int dx, int dy) {}
}
