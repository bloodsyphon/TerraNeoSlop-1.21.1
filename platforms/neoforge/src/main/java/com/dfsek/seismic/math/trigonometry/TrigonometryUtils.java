package com.dfsek.seismic.math.trigonometry;

// NeoForge dev/runtime can resolve TrigonometryFunctions from addon classloaders while
// this utility resolves from the Terra module classloader. Keep these access points public
// so cross-loader calls remain legal.
public class TrigonometryUtils {
    protected static final double a1 = 0.99997726;
    protected static final double a3 = -0.33262347;
    protected static final double a5 = 0.19354346;
    protected static final double a7 = -0.11643287;
    protected static final double a9 = 0.05265332;
    protected static final double a11 = -0.0117212;
    protected static final long INT_ARRAY_BASE = 0L;
    protected static final long INT_ARRAY_SHIFT = 0L;
    private static final double TAU = Math.PI * 2.0D;
    private static final int lookupBits = 16;
    static final int lookupTableSize = 1 << lookupBits;
    private static final int lookupTableSizeWithMargin = lookupTableSize + 1;
    private static final float tauOverLookupSize = (float) (TAU / lookupTableSize);
    public static final double radianToIndex = lookupTableSize / TAU;
    private static final int[] sinTable = new int[lookupTableSizeWithMargin];

    private TrigonometryUtils() {
    }

    public static double sinLookup(int index) {
        return Float.intBitsToFloat(sinTable[index & (lookupTableSize - 1)]);
    }

    static {
        for(int i = 0; i < lookupTableSizeWithMargin; i++) {
            double d = i * tauOverLookupSize;
            sinTable[i] = Float.floatToRawIntBits((float) StrictMath.sin(d));
        }

        for(int i = 0; i < 360; i += 90) {
            double d = Math.toRadians(i);
            sinTable[((int) (d * radianToIndex)) & (lookupTableSize - 1)] = Float.floatToRawIntBits((float) StrictMath.sin(d));
        }
    }
}
