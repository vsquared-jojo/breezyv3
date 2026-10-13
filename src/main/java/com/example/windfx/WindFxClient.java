package com.example.windfx;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;

/** Client-only ambient wind particles. Cheap by design: capped, outdoors only. */
public class WindFxClient implements ClientModInitializer {

    private static final int MIN_SKY_LIGHT = 12;
    private static final double DUST_VELOCITY_BOOST = 3.0; // dust decelerates quickly
    private static double spawnBudget = 0;

    public static final SimpleParticleType WIND_LINE = FabricParticleTypes.simple(true);

    @Override
    public void onInitializeClient() {
        WindFxConfig.load();
        Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath("windfx", "wind_line"), WIND_LINE);
        ParticleProviderRegistry.getInstance().register(WIND_LINE, WindLineParticle.Provider::new);
        ClientTickEvents.END_CLIENT_TICK.register(WindFxClient::tick);
    }

    private static void tick(Minecraft mc) {
        WindFxConfig cfg = WindFxConfig.get();
        if (!cfg.enabled || cfg.particlesPerSecond <= 0) return;

        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;
        if (level.dimensionType().hasCeiling()) return; // skip Nether

        long t = level.getGameTime();

        // Slowly drifting wind direction + gust waves
        double angle = 0.6 + Math.sin(t * 0.0007) * 0.8 + Math.sin(t * 0.0023) * 0.3;
        double gustWave = Math.max(0.0, Math.sin(t * 0.0045) * Math.sin(t * 0.0131 + 1.7));
        if (level.isRaining()) gustWave = Math.min(1.0, gustWave + 0.3);
        double gust = gustWave * cfg.gustStrength;

        double speed = (0.06 + 0.14 * Math.min(1.0, gust)) * cfg.windSpeed;
        double mult = switch (cfg.mode) {
            case DUST -> DUST_VELOCITY_BOOST;
            case LINES -> 1.0;
            default -> 1.0;
        };
        double dx = Math.cos(angle) * speed * mult;
        double dz = Math.sin(angle) * speed * mult;

        // particles per second -> per tick, with gusts adding variation (never fully zero)
        double perTick = cfg.particlesPerSecond / 20.0 * (0.5 + 0.5 * Math.min(1.0, gust) + 0.5 * (1 - cfg.gustStrength / 2.0));
        spawnBudget += perTick;
        int count = (int) spawnBudget;
        spawnBudget -= count;

        ParticleOptions type = switch (cfg.mode) {
            case DUST -> ParticleTypes.WHITE_ASH;
            case LINES -> WIND_LINE;
            default -> ParticleTypes.CHERRY_LEAVES;
        };

        RandomSource rand = level.getRandom();
        int r = cfg.radius;
        for (int i = 0; i < count; i++) {
            double x, y, z;
            if (cfg.mode == WindFxConfig.ParticleMode.LINES) {
                // distant, high-up swooshes all around you (the look of Windy / Breezy)
                double dist = r * (0.6 + 1.8 * rand.nextDouble());
                double a = rand.nextDouble() * Math.PI * 2;
                x = player.getX() + Math.cos(a) * dist;
                z = player.getZ() + Math.sin(a) * dist;
                double h = rand.nextDouble();
                y = player.getY() - 2.0 + 24.0 * h * h;
            } else {
                // spawn upwind so particles blow across the player's view
                x = player.getX() - Math.cos(angle) * r * 0.6 + (rand.nextDouble() * 2 - 1) * r;
                z = player.getZ() - Math.sin(angle) * r * 0.6 + (rand.nextDouble() * 2 - 1) * r;
                y = player.getY() + rand.nextDouble() * 5.0 - 1.0;
            }

            BlockPos pos = BlockPos.containing(x, y, z);
            if (!level.getBlockState(pos).isAir()) continue;
            if (level.getBrightness(LightLayer.SKY, pos) < MIN_SKY_LIGHT) continue;

            level.addParticle(type, x, y, z, dx, (rand.nextDouble() - 0.5) * 0.02, dz);
        }
    }
}
