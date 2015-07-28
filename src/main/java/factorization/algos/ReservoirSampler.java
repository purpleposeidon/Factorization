package factorization.algos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ReservoirSampler<E> implements Iterable<E> {
    private final ArrayList<E> samples;
    private final int desired;
    private final Random rand;
    private int given = 0;
    
    public ReservoirSampler(int desired, Random rand) {
        samples = new ArrayList<E>(desired);
        this.desired = desired;
        if (rand == null) rand = new Random();
        this.rand = rand;
    }
    
    public void give(E sample) {
        if (given++ < desired) {
            samples.add(sample);
        } else {
            int j = rand.nextInt(given);
            if (j < desired) {
                samples.set(j, sample);
            }
        }
    }
    
    public List<E> getSamples() {
        return samples;
    }

    @Override
    public Iterator<E> iterator() {
        return samples.iterator();
    }
    
    public int size() {
        return samples.size();
    }

    public void preGive(int given) {
        this.given = given;
    }
    
}
