package com.sylvan.presence.util;

import java.util.Random;

public class RandomHelper extends Random {
    public float nextBetween(final float min, final float max) {
        return min + this.nextFloat() * (max - min);
    }

    public int nextBetween(final int min, final int max) {
        return min + this.nextInt() * (max - min);
    }
}
