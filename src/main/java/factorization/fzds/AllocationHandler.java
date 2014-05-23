package factorization.fzds;


public class AllocationHandler {
    private final String name;
    private final int squishySize;
    
    public AllocationHandler(final String name, final int squishySize) {
        this.name = name;
        this.squishySize = squishySize;
    }
    
    
    /**See TAoCP 2.5; starting on page 435
     * 
     * A singly-linked list with references to the head of the list, and a 'rover' that walks the list as calls are made to search for free space.
     * 
     * The list grows upwards away from zero by inserting MemoryBlocks at the head.
     */
    private static class MemoryBlock {
        private int start, size;
        private boolean isFree;
        private MemoryBlock next;
        
        private void split(int needSize) {
            MemoryBlock remainder = new MemoryBlock();
            remainder.isFree = true;
            remainder.start = start;
            remainder.size = this.size - needSize;
            this.start += needSize;
            this.size = needSize;
            this.next = remainder;
        }
    }
    
    /**
     * This points to the head of the list. If the list can't fit the desired area, then we move the head away from 0.
     */
    private MemoryBlock head;
    
    /**
     * Keeps track of where we last looked; for efficiency
     */
    private MemoryBlock rover;
    
    public int allocate(int size) {
        return allocBlock(size).start;
    }
    
    private MemoryBlock allocBlock(int size) {
        if (head == null) {
            head = new MemoryBlock();
            head.start = size;
            head.size = size;
            head.isFree = false;
            head.next = null;
            return head;
        }
        //Check from rover to tail
        if (rover != null) {
            rover = tryTake(rover, null, size, squishySize);
            if (rover != null) return rover;
        }
        //Else check from head to rover
        rover = tryTake(head, rover, size, squishySize);
        if (rover != null) return rover;
        //Else sigh & make the head bigger
        return rover = extendArea(size);
    }
    
    private static MemoryBlock tryTake(MemoryBlock here, MemoryBlock end, int needSize, int squishyBugSize) {
        while (here != end) {
            if (here.isFree && needSize <= here.size) {
                here.isFree = false;
                if (here.size < needSize + squishyBugSize) {
                    //Would leave a uselessly tiny area behind
                    return here;
                }
                here.split(needSize);
                return here;
            }
            here = here.next;
        }
        return null;
    }
    
    private MemoryBlock extendArea(int needSize) {
        MemoryBlock ret = new MemoryBlock();
        ret.start = head.start + head.size;
        ret.size = needSize;
        ret.isFree = false;
        ret.next = head;
        head = ret;
        return ret;
    }
    
    public void free(int start) {
        if (findAndFree(rover, null, start)) {
            return;
        }
        if (findAndFree(head, rover, start)) {
            return;
        }
        System.err.println("AllocationHandler for " + name + " was asked to free area starting at " + start + ", but it was not found!");
    }
    
    private boolean findAndFree(MemoryBlock block, MemoryBlock endBlock, int start) {
        MemoryBlock prevBlock = null;
        do {
            if (block.start == start) {
                block.isFree = true;
                mergeContiguous(prevBlock != null ? prevBlock : block);
                return true;
            }
            prevBlock = block;
            block = block.next;
        } while (block != endBlock);
        return false;
    }
    
    private void mergeContiguous(MemoryBlock here) {
        while (collapse(here)) ;
    }
    
    private boolean collapse(MemoryBlock block) {
        if (block == null) {
            return false; //Uh oh!
        }
        if (block.next == null) {
            return false;
        }
        MemoryBlock after = block.next;
        if (block.isFree && after.isFree) {
            if (rover == after) {
                rover = after.next;
            }
            //We don't need to worry about head since head can't be eliminated this way
            
            block.start = after.start;
            block.size += after.size;
            block.next = after.next;
            return true;
        }
        return false;
    }
}
