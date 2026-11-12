package com.example.autosimplevoicedistance;

import com.example.autosimplevoicedistance.client.VoiceCalibrationScreen;
import com.example.autosimplevoicedistance.client.VoiceHudOverlay;
import com.example.autosimplevoicedistance.network.VolumePayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod("autosimplevoicedistance")
public class AutoSimpleVoiceDistanceMod {

    public AutoSimpleVoiceDistanceMod(IEventBus modEventBus, ModContainer container, Dist dist) {
        container.registerConfig(ModConfig.Type.CLIENT, VoiceConfig.CLIENT_SPEC);

        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);

        if (dist.isClient()) {
            container.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (modContainer, parent) -> new VoiceCalibrationScreen(parent)
            );
            modEventBus.addListener(this::registerGuiLayers);
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                VolumePayload.TYPE,
                VolumePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    float safeDistance = Math.clamp(payload.targetDistance(), 2.0f, 100.0f);
                    AutoVoicePlugin.PLAYER_DISTANCES.put(context.player().getUUID(), safeDistance);
                })
        );
    }

    private void registerGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("autosimplevoicedistance", "voice_hud"),
                new VoiceHudOverlay()
        );
    }

    private void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        AutoVoicePlugin.PLAYER_DISTANCES.remove(event.getEntity().getUUID());
    }
}
