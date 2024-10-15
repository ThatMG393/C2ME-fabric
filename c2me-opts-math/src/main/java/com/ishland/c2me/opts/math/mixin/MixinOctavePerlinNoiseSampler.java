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

    @Unique
    private int octaveSamplersCount = 0;

    @Unique
    private double[] amplitudesArray = null;

    // Fixed-point arithmetic
    private final long precisionFactor = 1 << 24;  // Equivalent to 16777216

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.octaveSamplersCount = this.octaveSamplers.length;
        this.amplitudesArray = this.amplitudes.toDoubleArray();
    }

    /**
     * @author ishland
     * @reason remove frequent type conversion
     */
    @Overwrite
    public static double maintainPrecision(double value) {
        return value - (((value + (precisionFactor >> 1)) >> 24) << 24);
    }

    /**
     * @author ishland
     * @reason optimize for common cases
     */
    @Overwrite
    public double sample(double x, double y, double z) {
        long d = 0;
        long e = (long) (this.lacunarity * precisionFactor); // Scale lacunarity to fixed-point
        long f = (long) (this.persistence * precisionFactor); // Scale persistence to fixed-point

        for (int i = 0; i < this.octaveSamplersCount; ++i) {
            PerlinNoiseSampler perlinNoiseSampler = this.octaveSamplers[i];
            if (perlinNoiseSampler != null) {
                long g = perlinNoiseSampler.sample(
                        maintainPrecisionFixed(x * e), maintainPrecisionFixed(y * e), maintainPrecisionFixed(z * e), 0, 0
                );
                // Convert back to double for the final sum
                d += (this.amplitudesArray[i] * g * f) >> 24; // Scale down to get the correct precision
            }

            e <<= 1; // Double e by shifting left (equivalent to multiplying by 2.0)
            f >>= 1; // Halve f by shifting right (equivalent to dividing by 2.0)
        }

        return (double) (d >> 48);
    }
}
