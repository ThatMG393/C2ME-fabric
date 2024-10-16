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

    private static final int FIXED_POINT_FACTOR = 1 << 16;

    private static int toFixedPoint(double value) {
        return (int) (value * FIXED_POINT_FACTOR);
    }

    private int fade(int t) {
        return ((t * ((t * t) >> 16) * (toFixedPoint(6.0) - toFixedPoint(15.0))) >> 16) + toFixedPoint(10.0);
    }

    @Deprecated
    @Overwrite
    public double sample(double x, double y, double z, double yScale, double yMax) {
        int fixedX = toFixedPoint(x + this.originX);
        int fixedY = toFixedPoint(y + this.originY);
        int fixedZ = toFixedPoint(z + this.originZ);

        int i = fixedX >> 16;
        int j = fixedY >> 16;
        int k = fixedZ >> 16;

        int g = fixedX & 0xFFFF;
        int h = fixedY & 0xFFFF;

        int o = (yScale != 0.0)
                ? ((yMax >= 0.0 && yMax < (h / (double) FIXED_POINT_FACTOR)) ? toFixedPoint(yMax) : h)
                : 0;

        return (double) sample(i, j, k, g, h - o, fixedZ & 0xFFFF, h);
    }
    
    @Overwrite
    private double sample(int sectionX, int sectionY, int sectionZ, int localX, int localY, int localZ, int fadeLocalX) {
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
        final int var12 = (var8 + sectionZ) & 0xFF;
        final int var13 = (var9 + sectionZ) & 0xFF;
        final int var14 = (var10 + sectionZ) & 0xFF;
        final int var15 = (var11 + sectionZ) & 0xFF;

        final int var16 = (this.permutation[var12] & 15) << 2;
        final int var17 = (this.permutation[var13] & 15) << 2;
        final int var18 = (this.permutation[var14] & 15) << 2;
        final int var19 = (this.permutation[var15] & 15) << 2;

        // Compute gradients
        final double grad0 = dotGrad(var16, localX, localY, localZ);
        final double grad1 = dotGrad(var17, localX - FIXED_POINT_FACTOR, localY, localZ);
        final double grad2 = dotGrad(var18, localX, localY - FIXED_POINT_FACTOR, localZ);
        final double grad3 = dotGrad(var19, localX - FIXED_POINT_FACTOR, localY - FIXED_POINT_FACTOR, localZ);

        // Fade calculations
        final int fadeX = fade(localX);
        final int fadeY = fade(localY);
        final int fadeZ = fade(localZ);

        // Interpolation
        double v0 = grad0 + ((fadeX * (grad1 - grad0)) >> 16);
        double v1 = grad2 + ((fadeX * (grad3 - grad2)) >> 16);
        return v0 + ((fadeZ * (v1 - v0)) >> 16);
    }

    // Gradient calculation
    private static double dotGrad(int gradIndex, int x, int y, int z) {
        return FLAT_SIMPLEX_GRAD[gradIndex] * x
                + FLAT_SIMPLEX_GRAD[gradIndex + 1] * y
                + FLAT_SIMPLEX_GRAD[gradIndex + 2] * z;
    }
}
