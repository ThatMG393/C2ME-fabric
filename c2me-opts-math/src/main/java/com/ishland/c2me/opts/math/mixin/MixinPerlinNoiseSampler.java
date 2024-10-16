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
    private static final int[] FLAT_SIMPLEX_GRAD = new int[]{
            65536, 65536, 0, 0,
            -65536, 65536, 0, 0,
            65536, -65536, 0, 0,
            -65536, -65536, 0, 0,
            65536, 0, 65536, 0,
            -65536, 0, 65536, 0,
            65536, 0, -65536, 0,
            -65536, 0, -65536, 0,
            0, 65536, 65536, 0,
            0, -65536, 65536, 0,
            0, 65536, -65536, 0,
            0, -65536, -65536, 0,
            65536, 65536, 0, 0,
            0, -65536, 65536, 0,
            -65536, 65536, 0, 0,
            0, -65536, -65536, 0,
    };

    @Unique
    private static final int FIXED_POINT_SCALE = 65536; // 2^16

    @Deprecated
    @Overwrite
    public double sample(double x, double y, double z, double yScale, double yMax) {
        int fixedX = (int) ((x + this.originX) * FIXED_POINT_SCALE);
        int fixedY = (int) ((y + this.originY) * FIXED_POINT_SCALE);
        int fixedZ = (int) ((z + this.originZ) * FIXED_POINT_SCALE);
        int sectionX = fixedX >> 16;
        int sectionY = fixedY >> 16;
        int sectionZ = fixedZ >> 16;
        int localX = fixedX & (FIXED_POINT_SCALE - 1);
        int localY = fixedY & (FIXED_POINT_SCALE - 1);
        int localZ = fixedZ & (FIXED_POINT_SCALE - 1);
        int fixedYScale = 0;
        if (yScale != 0.0) {
            int fixedYScaleTemp = (int)(yScale * FIXED_POINT_SCALE);
            int fixedYMax = (yMax >= 0.0 && yMax < (double)localY / FIXED_POINT_SCALE) 
                ? (int)(yMax * FIXED_POINT_SCALE) 
                : localY;
            long scaledY = ((long)fixedYMax << 32) / fixedYScaleTemp;
            fixedYScale = (int)(((scaledY + FIXED_POINT_SCALE) * fixedYScaleTemp) >> 32);
        }

        return (double) this.sample(sectionX, sectionY, sectionZ, localX, localY - fixedYScale, localZ, localY) / (FIXED_POINT_SCALE * FIXED_POINT_SCALE);
    }

    @Overwrite
    private int sample(int sectionX, int sectionY, int sectionZ, int localX, int localY, int localZ, int fadeLocalY) {
        int hash1 = this.permutation[sectionX & 0xFF] & 0xFF;
        int hash2 = this.permutation[(sectionX + 1) & 0xFF] & 0xFF;
        int hash3 = this.permutation[(hash1 + sectionY) & 0xFF] & 0xFF;
        int hash4 = this.permutation[(hash2 + sectionY) & 0xFF] & 0xFF;
        int hash5 = this.permutation[(hash1 + sectionY + 1) & 0xFF] & 0xFF;
        int hash6 = this.permutation[(hash2 + sectionY + 1) & 0xFF] & 0xFF;

        int grad000 = this.permutation[(hash3 + sectionZ) & 0xFF] & 15;
        int grad100 = this.permutation[(hash4 + sectionZ) & 0xFF] & 15;
        int grad010 = this.permutation[(hash5 + sectionZ) & 0xFF] & 15;
        int grad110 = this.permutation[(hash6 + sectionZ) & 0xFF] & 15;
        int grad001 = this.permutation[(hash3 + sectionZ + 1) & 0xFF] & 15;
        int grad101 = this.permutation[(hash4 + sectionZ + 1) & 0xFF] & 15;
        int grad011 = this.permutation[(hash5 + sectionZ + 1) & 0xFF] & 15;
        int grad111 = this.permutation[(hash6 + sectionZ + 1) & 0xFF] & 15;

        int inverseLocalX = FIXED_POINT_SCALE - localX;
        int inverseLocalY = FIXED_POINT_SCALE - localY;
        int inverseLocalZ = FIXED_POINT_SCALE - localZ;

        int noise000 = (FLAT_SIMPLEX_GRAD[grad000 << 2] * localX + FLAT_SIMPLEX_GRAD[(grad000 << 2) | 1] * localY + FLAT_SIMPLEX_GRAD[(grad000 << 2) | 2] * localZ) >> 16;
        int noise100 = (FLAT_SIMPLEX_GRAD[grad100 << 2] * inverseLocalX + FLAT_SIMPLEX_GRAD[(grad100 << 2) | 1] * localY + FLAT_SIMPLEX_GRAD[(grad100 << 2) | 2] * localZ) >> 16;
        int noise010 = (FLAT_SIMPLEX_GRAD[grad010 << 2] * localX + FLAT_SIMPLEX_GRAD[(grad010 << 2) | 1] * inverseLocalY + FLAT_SIMPLEX_GRAD[(grad010 << 2) | 2] * localZ) >> 16;
        int noise110 = (FLAT_SIMPLEX_GRAD[grad110 << 2] * inverseLocalX + FLAT_SIMPLEX_GRAD[(grad110 << 2) | 1] * inverseLocalY + FLAT_SIMPLEX_GRAD[(grad110 << 2) | 2] * localZ) >> 16;
        int noise001 = (FLAT_SIMPLEX_GRAD[grad001 << 2] * localX + FLAT_SIMPLEX_GRAD[(grad001 << 2) | 1] * localY + FLAT_SIMPLEX_GRAD[(grad001 << 2) | 2] * inverseLocalZ) >> 16;
        int noise101 = (FLAT_SIMPLEX_GRAD[grad101 << 2] * inverseLocalX + FLAT_SIMPLEX_GRAD[(grad101 << 2) | 1] * localY + FLAT_SIMPLEX_GRAD[(grad101 << 2) | 2] * inverseLocalZ) >> 16;
        int noise011 = (FLAT_SIMPLEX_GRAD[grad011 << 2] * localX + FLAT_SIMPLEX_GRAD[(grad011 << 2) | 1] * inverseLocalY + FLAT_SIMPLEX_GRAD[(grad011 << 2) | 2] * inverseLocalZ) >> 16;
        int noise111 = (FLAT_SIMPLEX_GRAD[grad111 << 2] * inverseLocalX + FLAT_SIMPLEX_GRAD[(grad111 << 2) | 1] * inverseLocalY + FLAT_SIMPLEX_GRAD[(grad111 << 2) | 2] * inverseLocalZ) >> 16;

        int fadeX = (localX * localX * localX * (localX * (localX * 6 - 15 * FIXED_POINT_SCALE) + 10 * FIXED_POINT_SCALE)) >> 32;
        int fadeY = (fadeLocalY * fadeLocalY * fadeLocalY * (fadeLocalY * (fadeLocalY * 6 - 15 * FIXED_POINT_SCALE) + 10 * FIXED_POINT_SCALE)) >> 32;
        int fadeZ = (localZ * localZ * localZ * (localZ * (localZ * 6 - 15 * FIXED_POINT_SCALE) + 10 * FIXED_POINT_SCALE)) >> 32;

        int lerp00 = noise000 + (((long)(noise100 - noise000) * fadeX) >> 16);
        int lerp10 = noise010 + (((long)(noise110 - noise010) * fadeX) >> 16);
        int lerp01 = noise001 + (((long)(noise101 - noise001) * fadeX) >> 16);
        int lerp11 = noise011 + (((long)(noise111 - noise011) * fadeX) >> 16);

        int lerpX0 = lerp00 + (((long)(lerp10 - lerp00) * fadeY) >> 16);
        int lerpX1 = lerp01 + (((long)(lerp11 - lerp01) * fadeY) >> 16);

        return lerpX0 + (((long)(lerpX1 - lerpX0) * fadeZ) >> 16);
    }
}
