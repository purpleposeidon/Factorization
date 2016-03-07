package factorization.algos;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import factorization.util.NORELEASE;

import java.util.ArrayList;
import java.util.Random;

public class ToothArray {
    private final byte data[];

    public ToothArray(int count) {
        if (count % 4 != 0) throw new IllegalArgumentException("count must be divisible by 4");
        this.data = new byte[count / 4];
    }

    public byte get(int index) {
        int bigIndex = index >> 2;
        int lilIndex = (index & 0x3) << 1;
        if (bigIndex < 0 || bigIndex >= data.length) return -1;
        byte packed = data[bigIndex];
        return (byte) (packed >> lilIndex & 0x3);
    }

    public void set(int index, byte val) {
        if (NORELEASE.on) {
            if ((val & 0x3) != val) throw new AssertionError("Value out of range: " + val);
        }
        int bigIndex = index >> 2;
        int lilIndex = (index & 0x3) << 1;
        if (bigIndex < 0 || bigIndex >= data.length) return;
        byte packed = data[bigIndex];
        packed &= ~(0x3 << lilIndex);
        packed |= val << lilIndex;
        data[bigIndex] = packed;
    }


    private static class Test {
        ToothArray mouth;
        byte[] mirror;

        Test(int size) {
            mouth = new ToothArray(size);
            mirror = new byte[size];
        }

        void set(int i, byte val) {
            byte origVal = mouth.get(i);
            if (origVal != mirror[i]) {
                throw new AssertionError("Value didn't match mirror!");
            }
            {
                byte below = mouth.get(i - 1);
                byte above = mouth.get(i + 1);
                {
                    mouth.set(i, val);
                }
                if (mouth.get(i - 1) != below) {
                    throw new AssertionError("Lower neighbor changed!");
                }
                if (mouth.get(i + 1) != above) {
                    throw new AssertionError("Higher neighbor changed!");
                }
            }
            byte newVal = mouth.get(i);
            if (newVal != val) {
                throw new AssertionError("Modification failed!");
            }
            mirror[i] = val;
        }

        byte get(int i) {
            byte ret = mouth.get(i);
            if (ret != mirror[i]) {
                throw new AssertionError("mirror didn't match!");
            }
            return ret;
        }

        static void drive(int len, ArrayList<Function<Test, Void>> acts) {
            Test t = new Test(len);
            for (int i = 0, actsSize = acts.size(); i < actsSize; i++) {
                Function<Test, Void> a = acts.get(i);
                a.apply(t);
            }
            t = new Test(len + 4);
            for (Function<Test, Void> a : acts) {
                a.apply(t);
            }
        }

        static void fillTest(int len, byte val) {
            Test t = new Test(len);
            for (int i = 0; i < len; i++) {
                t.set(i, val);
            }
            for (int i = 0; i < len; i++) {
                t.get(i);
            }
        }

        public static void main(String[] args) {
            fillTest(12, (byte) 1);
            fillTest(24, (byte) 2);
            fillTest(40, (byte) 3);
            randomTest(599, 1000);
        }

        protected static void randomTest(final long seed, final int rounds) {
            for (int round = 0; round < rounds; round++) {
                System.out.println("Round " + round);
                Random rand = new Random(seed * 1000000 + round);
                int len = 4 + (rand.nextInt(8) * 4);
                ArrayList<Function<Test, Void>> acts = Lists.newArrayList();
                int N = rand.nextInt(100000 * len);
                while (N --> 0) {
                    Function<Test, Void> act;
                    final int i = rand.nextInt(len);
                    if (rand.nextBoolean()) {
                        act = input -> {
                            input.get(i);
                            return null;
                        };
                    } else {
                        final byte val = (byte) rand.nextInt(0x3);
                        act = input -> {
                            input.set(i, val);
                            return null;
                        };
                    }
                    acts.add(act);
                }
                drive(len, acts);
            }
        }


    }
}
