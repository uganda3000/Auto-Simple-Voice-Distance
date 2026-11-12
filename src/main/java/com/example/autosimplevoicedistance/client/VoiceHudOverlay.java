package com.example.autosimplevoicedistance.client;

import com.example.autosimplevoicedistance.AutoVoicePlugin;
import com.example.autosimplevoicedistance.VoiceConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

public class VoiceHudOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!VoiceConfig.CLIENT.enabled.get() || !VoiceConfig.CLIENT.showHud.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        float dist = AutoVoicePlugin.currentSmoothedDistance;
        float wDist = VoiceConfig.CLIENT.whisperDistance.get().floatValue();
        float sDist = VoiceConfig.CLIENT.shoutDistance.get().floatValue();

        String label;
        int color;
        if (dist <= wDist + 3.0f) {
            label = String.format("Шёпот (%.0f м)", dist);
            color = 0x88CCFF;
        } else if (dist >= sDist - 8.0f) {
            label = String.format("Крик (%.0f м)", dist);
            color = 0xFF5555;
        } else {
            label = String.format("Голос (%.0f м)", dist);
            color = 0xFFFFFF;
        }

        int x = 10;
        int y = graphics.guiHeight() - 36;
        graphics.drawString(mc.font, label, x, y, color, true);
    }
}
