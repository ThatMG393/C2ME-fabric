package com.ishland.c2me.opts.math.mixin;

import net.minecraft.util.math.noise.PerlinNoiseSampler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = PerlinNoiseSampler.class, priority = 1090)
public abstract class MixinPerlinNoiseSampler {

    @Shadow @Final public double originY;
    @Shadow @Final public double originX;
    @Shadow @Final public double originZ;
    @Shadow @Final private byte[] permutation;

    @Unique
    private static final double[] FLAT_SIMPLEX_GRAD = new double[]{
            1, 1, 0, 0,
            -1, 1, 0, 0,
            1, -1, 0, 0,
            -1, -1, 0, 0,
            1, 0, 1, 0,
            -1, 0, 1, 0,
            1, 0, -1, 0,
            -1, 0, -1, 0,
            0, 1, 1, 0,
            0, -1, 1, 0,
            0, 1, -1, 0,
            0, -1, -1, 0,
            1, 1, 0, 0,
            0, -1, 1, 0,
            -1, 1, 0, 0,
            0, -1, -1, 0,
    };

    private static final int FIXED_POINT_FACTOR = 1 << 16; // 65536

    // Convert double to fixed-point (16.16)
    private static int toFixedPoint(double value) {
        return (int) (value * FIXED_POINT_FACTOR);
    }

    // Fade function using fixed-point arithmetic with inlined calculations
    private int fade(int t) {
        return ((t * ((t * t) >> 16) * (toFixedPoint(6.0) - toFixedPoint(15.0)) >> 16) + toFixedPoint(10.0)) >> 16);
    }

    /**
     * @author ishland
     * @reason optimize: remove frequent type conversions
     */
    @Deprecated
    @Overwrite
    public double sample(double x, double y, double z, double yScale, double yMax) {
        int fixedX = toFixedPoint(x + this.originX);
        int fixedY = toFixedPoint(y + this.originY);
        int fixedZ = toFixedPoint(z + this.originZ);

        int i = fixedX >> 16; // Integer part of fixedX
        int j = fixedY >> 16; // Integer part of fixedY
        int k = fixedZ >> 16; // Integer part of fixedZ
        int g = fixedX & 0xFFFF; // Fractional part of fixedX
        int h = fixedY & 0xFFFF; // Fractional part of fixedY

        // Determine o using fixed-point without division
        int o = (yScale != 0.0) ? (int) ((yMax >= 0.0 && yMax < (h / (double) FIXED_POINT_FACTOR)) ? toFixedPoint(yMax) : h) / toFixedPoint(yScale) * toFixedPoint(yScale) + 0.5) : 0;

        return (double) sample(i, j, k, g, h - o, fixedZ & 0xFFFF, h);
    }

    /**
     * @author ishland
     * @reason inline math & small optimization: remove frequent type conversions and redundant ops
     */
    @Overwrite
    private double sample(int sectionX, int sectionY, int sectionZ, int localX, int localY, int localZ, int fadeLocalX) {
        // Calculate indices with inlining
        final int var0 = sectionX & 0xFF;
        final int var1 = (sectionX + 1) & 0xFF;
        final int var2 = this.permutation[var0] & 0xFF;
        final int var3 = this.permutation[var1] & 0xFF;
        final int var4 = (var2 + sectionY) & 0xFF;
        final int var5 = (var3 + sectionY) & 0xFF;
        final int var6 = (var2 + sectionY + 1) & 0xFF;
        final int var7 = (var3 + sectionY + 1) & 0xFF;

        final int var8 = this.permutation[var4] & 0xFF;
        final int var9 = this.permutation[var5] & 0xFF;
        final int var10 = this.permutation[var6] & 0xFF;
        final int var11 = this.permutation[var7] & 0xFF;

        // Calculate gradient indices with inlining
        final int var12 = (var8 + sectionZ) & 0xFF;
        final int var13 = (var9 + sectionZ) & 0xFF;
        final int var14 = (var10 + sectionZ) & 0xFF;
        final int var15 = (var11 + sectionZ) & 0xFF;
        final int var16 = (this.permutation[var12] & 15) << 2;
        final int var17 = (this.permutation[var13] & 15) << 2;
        final int var18 = (this.permutation[var14] & 15) << 2;
        final int var19 = (this.permutation[var15] & 15) << 2;

        // Local coordinates as double for gradient calculations
        double localXDouble = (double) localX / FIXED_POINT_FACTOR;
        double localYDouble = (double) localY / FIXED_POINT_FACTOR;
        double localZDouble = (double) localZ / FIXED_POINT_FACTOR;
        double localX1 = localXDouble - 1.0;
        double localY1 = localYDouble - 1.0;

        // Gradient calculations using fixed-point
        double grad0 = FLAT_SIMPLEX_GRAD[(var16) | 0] * localXDouble + FLAT_SIMPLEX_GRAD[(var16) | 1] * localYDouble + FLAT_SIMPLEX_GRAD[(var16) | 2] * localZDouble;
        double grad1 = FLAT_SIMPLEX_GRAD[(var17) | 0] * localX1 + FLAT_SIMPLEX_GRAD[(var17) | 1] * localYDouble + FLAT_SIMPLEX_GRAD[(var17) | 2] * localZDouble;
        double grad2 = FLAT_SIMPLEX_GRAD[(var18) | 0] * localXDouble + FLAT_SIMPLEX_GRAD[(var18) | 1] * localY1 + FLAT_SIMPLEX_GRAD[(var18) | 2] * localZDouble;
        double grad3 = FLAT_SIMPLEX_GRAD[(var19) | 0] * localX1 + FLAT_SIMPLEX_GRAD[(var19) | 1] * localY1 + FLAT_SIMPLEX_GRAD[(var19) | 2] * localZDouble;

        // Fade calculations using fixed-point
        int fadeX = fade(localX);
        int fadeY = fade(localY);
        int fadeZ = fade(localZ);

        // Interpolation without divisions
        double v0 = grad0 + ((fadeX * (grad1 - grad0)) >> 16);
        double v1 = grad2 + ((fadeX * (grad3 - grad2)) >> 16);
        return v0 + ((fadeZ * (v1 - v0)) >> 16);
    }
}
