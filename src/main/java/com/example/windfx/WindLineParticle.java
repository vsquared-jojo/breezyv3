package com.example.windfx;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A thin pixel-art wind swoosh that draws itself on, curls, and fades out.
 *
 * Its direction is fixed in the WORLD (the real wind direction at the moment it spawned),
 * so it does not swing around when you turn your camera. Only its flat side is turned
 * toward you (rotation around the wind axis).
 */
public class WindLineParticle extends SingleQuadParticle {

    private final FabricSpriteSet sprites;
    private final float dirX, dirY, dirZ; // unit wind direction in the world

    protected WindLineParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz, FabricSpriteSet sprites) {
        super(level, x, y, z, sprites.get(level.getRandom()));
        this.sprites = sprites;
        WindFxConfig cfg = WindFxConfig.get();
        RandomSource r = level.getRandom();

        // world-space direction of this swoosh = the wind it was born into (+ tiny vertical tilt)
        double len = Math.sqrt(vx * vx + vz * vz);
        double hx = len > 1.0E-6 ? vx / len : 1.0;
        double hz = len > 1.0E-6 ? vz / len : 0.0;
        double hy = (r.nextDouble() - 0.5) * 0.25;
        double n = Math.sqrt(hx * hx + hy * hy + hz * hz);
        this.dirX = (float) (hx / n);
        this.dirY = (float) (hy / n);
        this.dirZ = (float) (hz / n);

        // drift along that same direction at wind speed
        this.xd = vx * 0.8;
        this.yd = 0.0;
        this.zd = vz * 0.8;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;

        // wind speed setting = animation speed (higher = shorter life = faster swoosh)
        double life = (38 + r.nextInt(22)) / Math.max(0.1, cfg.windSpeed);
        this.lifetime = (int) Math.max(12, Math.min(160, life));

        this.quadSize = (2.2F + r.nextFloat() * 2.0F) * cfg.lineLength;

        this.setColor(1.0F, 1.0F, 1.0F);
        this.setAlpha(0.75F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAlive()) {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        double px = Mth.lerp(partialTick, this.xo, this.x);
        double py = Mth.lerp(partialTick, this.yo, this.y);
        double pz = Mth.lerp(partialTick, this.zo, this.z);
        Vec3 cam = camera.position();

        Vector3f right = new Vector3f(this.dirX, this.dirY, this.dirZ);          // swoosh heads this way
        Vector3f toCam = new Vector3f((float) (cam.x - px), (float) (cam.y - py), (float) (cam.z - pz));

        // face the camera as much as possible while keeping the wind axis fixed
        float along = toCam.dot(right);
        Vector3f normal = new Vector3f(toCam).sub(right.x * along, right.y * along, right.z * along);
        if (normal.lengthSquared() < 1.0E-6F) {          // looking exactly along the wind
            normal.set(0.0F, 1.0F, 0.0F);
            float a2 = normal.dot(right);
            normal.sub(right.x * a2, right.y * a2, right.z * a2);
            if (normal.lengthSquared() < 1.0E-6F) normal.set(1.0F, 0.0F, 0.0F);
        }
        normal.normalize();                              // points toward the viewer
        Vector3f up = new Vector3f(normal).cross(right); // sprite "up"

        // quad local axes: +X = sprite left, +Y = sprite up, +Z = away from viewer
        Matrix3f basis = new Matrix3f();
        basis.setColumn(0, new Vector3f(right).negate());
        basis.setColumn(1, up);
        basis.setColumn(2, new Vector3f(normal).negate());
        Quaternionf q = new Quaternionf().setFromNormalized(basis);

        this.extractRotatedQuad(state, camera, q, partialTick);
    }

    // Full brightness so the wind stays visible at night (both names covered on purpose;
    // whichever one this Minecraft version calls is the one that takes effect).
    public int getLightColor(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public record Provider(FabricSpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz, RandomSource random) {
            return new WindLineParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
