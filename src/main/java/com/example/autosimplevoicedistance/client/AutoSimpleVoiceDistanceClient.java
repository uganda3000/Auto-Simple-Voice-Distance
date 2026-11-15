package com.example.autosimplevoicedistance.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = "autosimplevoicedistance", dist = Dist.CLIENT)
public class AutoSimpleVoiceDistanceClient {

    public AutoSimpleVoiceDistanceClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new VoiceCalibrationScreen(parent)
        );
        modEventBus.addListener(this::registerGuiLayers);
    }

    private void registerGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("autosimplevoicedistance", "voice_hud"),
                new VoiceHudOverlay()
        );
    }
}
