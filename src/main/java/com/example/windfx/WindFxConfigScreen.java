package com.example.windfx;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WindFxConfigScreen {

    public static Screen create(Screen parent) {
        WindFxConfig def = new WindFxConfig();
        WindFxConfig cfg = WindFxConfig.get();

        Option<Boolean> enabled = Option.<Boolean>createBuilder()
                .name(Component.literal("Enable wind particles"))
                .description(OptionDescription.of(Component.literal("Master switch.")))
                .binding(def.enabled, () -> cfg.enabled, v -> cfg.enabled = v)
                .controller(BooleanControllerBuilder::create)
                .build();

        Option<Integer> pps = Option.<Integer>createBuilder()
                .name(Component.literal("Particles per second"))
                .description(OptionDescription.of(Component.literal(
                        "Main FPS knob. Lower = faster. 10-30 looks natural for wind lines.")))
                .binding(def.particlesPerSecond, () -> cfg.particlesPerSecond, v -> cfg.particlesPerSecond = v)
                .controller(o -> IntegerSliderControllerBuilder.create(o).range(0, 200).step(5))
                .build();

        Option<Float> speed = Option.<Float>createBuilder()
                .name(Component.literal("Wind speed"))
                .description(OptionDescription.of(Component.literal(
                        "Wind lines: how fast each swoosh animates. Dust: how fast flecks blow. Leaves use vanilla physics.")))
                .binding(def.windSpeed, () -> cfg.windSpeed, v -> cfg.windSpeed = v)
                .controller(o -> FloatSliderControllerBuilder.create(o).range(0.1f, 4f).step(0.1f)
                        .formatValue(v -> Component.literal(String.format("%.1fx", v))))
                .build();

        Option<Float> length = Option.<Float>createBuilder()
                .name(Component.literal("Wind line size"))
                .description(OptionDescription.of(Component.literal(
                        "How big each wind swoosh is. Only affects the Wind lines style.")))
                .binding(def.lineLength, () -> cfg.lineLength, v -> cfg.lineLength = v)
                .controller(o -> FloatSliderControllerBuilder.create(o).range(0.3f, 3f).step(0.1f)
                        .formatValue(v -> Component.literal(String.format("%.1fx", v))))
                .build();

        Option<Integer> radius = Option.<Integer>createBuilder()
                .name(Component.literal("Spawn distance (blocks)"))
                .description(OptionDescription.of(Component.literal(
                        "Wind lines appear at 0.6x to 2.4x this distance around you. Bigger = more distant wind.")))
                .binding(def.radius, () -> cfg.radius, v -> cfg.radius = v)
                .controller(o -> IntegerSliderControllerBuilder.create(o).range(8, 40).step(1))
                .build();

        Option<Float> gust = Option.<Float>createBuilder()
                .name(Component.literal("Gust strength"))
                .description(OptionDescription.of(Component.literal(
                        "How much the wind swells and calms over time. 0 = constant breeze.")))
                .binding(def.gustStrength, () -> cfg.gustStrength, v -> cfg.gustStrength = v)
                .controller(o -> FloatSliderControllerBuilder.create(o).range(0f, 2f).step(0.1f)
                        .formatValue(v -> Component.literal(String.format("%.1fx", v))))
                .build();

        Option<WindFxConfig.ParticleMode> mode = Option.<WindFxConfig.ParticleMode>createBuilder()
                .name(Component.literal("Particle style"))
                .description(OptionDescription.of(Component.literal(
                        "Wind lines: thin flowing streaks. Leaves: cherry-leaf look. Dust: small flecks.")))
                .binding(def.mode, () -> cfg.mode, v -> cfg.mode = v)
                .controller(o -> EnumControllerBuilder.create(o).enumClass(WindFxConfig.ParticleMode.class))
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Wind FX"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Wind"))
                        .option(enabled)
                        .option(pps)
                        .option(speed)
                        .option(length)
                        .option(radius)
                        .option(gust)
                        .option(mode)
                        .build())
                .save(WindFxConfig::save)
                .build()
                .generateScreen(parent);
    }
}
