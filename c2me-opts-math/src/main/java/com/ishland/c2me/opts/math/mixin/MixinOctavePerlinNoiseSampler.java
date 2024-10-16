package com.ishland.c2me.opts.math.mixin;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OctavePerlinNoiseSampler.class)
public class MixinOctavePerlinNoiseSampler {

    @Shadow @Final private double lacunarity;
    @Shadow @Final private double persistence;
    @Shadow @Final private PerlinNoiseSampler[] octaveSamplers;
    @Shadow @Final private DoubleList amplitudes;

    @Unique private static final long FIXED_POINT_SCALE = 1L << 24; // 2^24
    @Unique private static final long MODULUS = 562683007180800L; // 3.3554432E7 * FIXED_POINT_SCALE
    @Unique private int octaveSamplersCount = 0;
    @Unique private int[] fixedAmplitudes = null;
    @Unique private long fixedLacunarity; // Changed to long
    @Unique private long fixedPersistence; // Changed to long

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.octaveSamplersCount = this.octaveSamplers.length;
        this.fixedLacunarity = (long) (this.lacunarity * FIXED_POINT_SCALE);
        this.fixedPersistence = (long) (this.persistence * FIXED_POINT_SCALE);

        this.fixedAmplitudes = new int[this.octaveSamplersCount];
        for (int i = 0; i < this.octaveSamplersCount; ++i)
            this.fixedAmplitudes[i] = (int) (this.amplitudes.getDouble(i) * FIXED_POINT_SCALE);
    }

    /**
     * @author ishland
     * @reason optimize using fixed-point arithmetic
     */
    @Overwrite
    public static double maintainPrecision(double value) {
        long fixedValue = (long) (value * FIXED_POINT_SCALE);
        fixedValue = fixedValue - (fixedValue / MODULUS + (fixedValue >= 0 ? 1 : -1) >> 1) * MODULUS; 
        return (double) fixedValue >> 24; // Use right shift instead of division for final return
    }

    /**
     * @author ishland
     * @reason optimize using fixed-point arithmetic
     */
    @Overwrite
    public double sample(double x, double y, double z) {
        long fixedResult = 0;
        long fixedE = this.fixedLacunarity; // Keep as long to avoid overflow.
        long fixedF = this.fixedPersistence; // Keep as long to avoid overflow.

        for (int i = 0; i < this.octaveSamplersCount; ++i) {
            PerlinNoiseSampler perlinNoiseSampler = this.octaveSamplers[i];
            if (perlinNoiseSampler != null) {
                // Replace division by FIXED_POINT_SCALE with bitwise shift
                double maintainedX = maintainPrecision(x * (fixedE >> 24)); // Right shift to divide by FIXED_POINT_SCALE
                double maintainedY = maintainPrecision(y * (fixedE >> 24));
                double maintainedZ = maintainPrecision(z * (fixedE >> 24));
                
                @SuppressWarnings("deprecation")
                double g = perlinNoiseSampler.sample(maintainedX, maintainedY, maintainedZ, 0.0, 0.0);
                
                // Calculate the fixed result using left shift for multiplying
                fixedResult += (long) g * this.fixedAmplitudes[i] * (fixedF >> 24); // Right shift to divide by FIXED_POINT_SCALE
            }

            // Use bitwise shifts for fixedE and fixedF where appropriate.
            fixedE <<= 1;  // fixedE *= 2 (equivalent to a left shift)
            fixedF >>= 1;  // fixedF /= 2 (equivalent to a right shift)
        }

        return (double) fixedResult >> 24; // Use right shift instead of division for final return
    }
}
