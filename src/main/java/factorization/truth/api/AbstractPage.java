package factorization.truth.api;

import factorization.truth.DocViewer;

public abstract class AbstractPage {
    public abstract void draw(DocViewer doc, int ox, int oy, String hoveredLink);
    public void closed() {}
    public void mouseDragStart() {}
    public void mouseDrag(int dx, int dy) {}
}
