package factorization.colossi;

import factorization.api.Coord;
import net.minecraft.util.EnumFacing;

public class MaskTemplate {
    int width;
    String[] template;
    EnumFacing anchor = null;
    int anchor_points = 0;
    int anchor_x, anchor_y;
    
    int weight = 100;
    char MASK = '#', AIR = '.', EYE = '@';
    
    public MaskTemplate(String... template) {
        this.template = template;
        this.width = template[0].length();
        int START = 0;
        int END = template.length - 1;
        for (int lineIndex = START; lineIndex <= END; lineIndex++) {
            String s = template[lineIndex];
            if (s.length() != width) {
                throw new IllegalArgumentException("Template has inconsistent width");
            }
            for (int i = 0; i < width; i++) {
                char c = s.charAt(i);
                EnumFacing fd = toAnchor(c);
                if (fd == null) {
                    if (c == MASK || c == EYE || c == AIR) continue;
                    throw new IllegalArgumentException("Invalid template character: '" + c + "'");
                } else {
                    if (anchor != fd) {
                        if (anchor != null) {
                            throw new IllegalArgumentException("Mixed anchor types");
                        }
                        anchor = fd;
                        anchor_x = i;
                        anchor_y = END - lineIndex;
                    }
                    anchor_points++;
                }
            }
        }
    }
    
    EnumFacing toAnchor(char c) {
        switch (c) {
        case 'V':
        case 'v': return EnumFacing.DOWN;
        case '^': return EnumFacing.UP;
        case '<': return EnumFacing.NORTH;
        case '>': return EnumFacing.SOUTH;
        default: return null;
        } 
    }
    
    @Override
    public String toString() {
        String ret = "weight " + weight;
        for (String s : template) {
            ret += "\n" + s;
        }
        ret += "\n' width " + width;
        ret += "\n' anchor " + anchor;
        ret += "\n' anchor_points " + anchor_points;
        return ret;
    }

    public void paint(Coord anchor, Brush mask, Brush eye) {
        for (int ty = 0; ty < template.length; ty++) {
            for (int tz = 0; tz < width; tz++) {
                char c = template[ty].charAt(tz);
                if (c == '@' || c == '#') {
                    Coord here = anchor.add(0, template.length - ty - anchor_y, tz - anchor_x);
                    if (c == '@') {
                        eye.paint(here);
                    } else {
                        mask.paint(here);
                    }
                }
            }
        }
    }
}
