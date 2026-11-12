package com.example.autosimplevoicedistance.client;

import com.example.autosimplevoicedistance.AutoVoicePlugin;
import com.example.autosimplevoicedistance.VoiceConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class VoiceCalibrationScreen extends Screen {

    private final Screen parent;
    private int calibrationStep = 0;
    private long stepStartTime = 0L;
    private final List<Double> samples = new ArrayList<>();

    private Button calibrateBtn;
    private DbSlider whisperSlider;
    private DbSlider normalSlider;
    private DbSlider shoutSlider;

    public VoiceCalibrationScreen(Screen parent) {
        super(Component.literal("Настройки Auto Simple Voice Distance"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 38;

        this.addRenderableWidget(CycleButton.onOffBuilder(VoiceConfig.CLIENT.enabled.get())
            .create(centerX - 110, y, 220, 20, Component.literal("Авто-радиус голоса"),
                (btn, val) -> VoiceConfig.CLIENT.enabled.set(val)));
        y += 24;

        this.addRenderableWidget(CycleButton.onOffBuilder(VoiceConfig.CLIENT.showHud.get())
            .create(centerX - 110, y, 220, 20, Component.literal("Индикатор на экране (HUD)"),
                (btn, val) -> VoiceConfig.CLIENT.showHud.set(val)));
        y += 28;

        this.calibrateBtn = Button.builder(Component.literal("Автокалибровка микрофона (9 сек)"), btn -> startCalibration())
            .bounds(centerX - 110, y, 220, 20)
            .build();
        this.addRenderableWidget(this.calibrateBtn);
        y += 28;

        this.whisperSlider = new DbSlider(centerX - 110, y, 220, 20, "Порог шёпота",
            VoiceConfig.CLIENT.whisperDb.get(), -60.0, -5.0,
            val -> {
                VoiceConfig.CLIENT.whisperDb.set(val);
                if (VoiceConfig.CLIENT.normalDb.get() < val + 2.0) {
                    double newNorm = Mth.clamp(val + 2.0, -58.0, -2.0);
                    VoiceConfig.CLIENT.normalDb.set(newNorm);
                    if (normalSlider != null) normalSlider.setDbValue(newNorm);
                }
            });
        this.addRenderableWidget(this.whisperSlider);
        y += 24;

        this.normalSlider = new DbSlider(centerX - 110, y, 220, 20, "Обычная речь",
            VoiceConfig.CLIENT.normalDb.get(), -58.0, -2.0,
            val -> {
                VoiceConfig.CLIENT.normalDb.set(val);
                if (VoiceConfig.CLIENT.whisperDb.get() > val - 2.0) {
                    double newWhisp = Mth.clamp(val - 2.0, -60.0, -5.0);
                    VoiceConfig.CLIENT.whisperDb.set(newWhisp);
                    if (whisperSlider != null) whisperSlider.setDbValue(newWhisp);
                }
                if (VoiceConfig.CLIENT.shoutDb.get() < val + 2.0) {
                    double newShout = Mth.clamp(val + 2.0, -55.0, 0.0);
                    VoiceConfig.CLIENT.shoutDb.set(newShout);
                    if (shoutSlider != null) shoutSlider.setDbValue(newShout);
                }
            });
        this.addRenderableWidget(this.normalSlider);
        y += 24;

        this.shoutSlider = new DbSlider(centerX - 110, y, 220, 20, "Порог крика",
            VoiceConfig.CLIENT.shoutDb.get(), -55.0, 0.0,
            val -> {
                VoiceConfig.CLIENT.shoutDb.set(val);
                if (VoiceConfig.CLIENT.normalDb.get() > val - 2.0) {
                    double newNorm = Mth.clamp(val - 2.0, -58.0, -2.0);
                    VoiceConfig.CLIENT.normalDb.set(newNorm);
                    if (normalSlider != null) normalSlider.setDbValue(newNorm);
                }
            });
        this.addRenderableWidget(this.shoutSlider);

        this.addRenderableWidget(Button.builder(Component.literal("Сохранить и закрыть"), btn -> this.onClose())
            .bounds(centerX - 110, this.height - 30, 220, 20)
            .build());
    }

    private void startCalibration() {
        this.calibrationStep = 1;
        this.stepStartTime = System.currentTimeMillis();
        this.samples.clear();
        this.calibrateBtn.active = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (calibrationStep == 0) return;

        double db = AutoVoicePlugin.currentDb;
        if (db > -58.0) {
            samples.add(db);
        }

        long elapsed = System.currentTimeMillis() - stepStartTime;
        if (elapsed >= 3000L) {
            finishCurrentStep();
        }
    }

    private void finishCurrentStep() {
        double avgDb = samples.stream().mapToDouble(Double::doubleValue).average().orElse(-35.0);

        if (calibrationStep == 1) {
            double wVal = Math.round(Mth.clamp(avgDb + 2.0, -58.0, -10.0));
            VoiceConfig.CLIENT.whisperDb.set(wVal);
            whisperSlider.setDbValue(wVal);
            calibrationStep = 2;
        } else if (calibrationStep == 2) {
            double minNormal = VoiceConfig.CLIENT.whisperDb.get() + 4.0;
            double nVal = Math.round(Mth.clamp(avgDb, minNormal, -5.0));
            VoiceConfig.CLIENT.normalDb.set(nVal);
            normalSlider.setDbValue(nVal);
            calibrationStep = 3;
        } else if (calibrationStep == 3) {
            double minShout = VoiceConfig.CLIENT.normalDb.get() + 4.0;
            double sVal = Math.round(Mth.clamp(avgDb - 2.0, minShout, 0.0));
            VoiceConfig.CLIENT.shoutDb.set(sVal);
            shoutSlider.setDbValue(sVal);
            calibrationStep = 0;
            calibrateBtn.active = true;
            VoiceConfig.CLIENT_SPEC.save();
        }

        samples.clear();
        stepStartTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, 14, 0xFFFFFF);

        int barWidth = 220;
        int barX = centerX - barWidth / 2;
        int barY = 195;
        graphics.fill(barX, barY, barX + barWidth, barY + 12, 0xFF222222);

        double normalized = Mth.clamp((AutoVoicePlugin.currentDb + 60.0) / 60.0, 0.0, 1.0);
        int fillWidth = (int) (barWidth * normalized);
        graphics.fill(barX, barY, barX + fillWidth, barY + 12, 0xFF55FF55);

        drawMarker(graphics, barX, barY, barWidth, VoiceConfig.CLIENT.whisperDb.get(), 0xFF88CCFF);
        drawMarker(graphics, barX, barY, barWidth, VoiceConfig.CLIENT.normalDb.get(), 0xFFFFFFFF);
        drawMarker(graphics, barX, barY, barWidth, VoiceConfig.CLIENT.shoutDb.get(), 0xFFFF5555);

        if (calibrationStep > 0) {
            long secLeft = 3 - ((System.currentTimeMillis() - stepStartTime) / 1000L);
            String prompt = switch (calibrationStep) {
                case 1 -> "Шаг 1/3: ПОШЕПЧИТЕ в микрофон (" + secLeft + " с)";
                case 2 -> "Шаг 2/3: Поговорите ОБЫЧНЫМ голосом (" + secLeft + " с)";
                case 3 -> "Шаг 3/3: Громко КРИКНИТЕ в микрофон (" + secLeft + " с)";
                default -> "";
            };
            graphics.drawCenteredString(this.font, prompt, centerX, 214, 0xFFFF55);
        } else {
            String liveInfo = String.format("Микрофон: %.0f дБ  |  Текущий радиус: %.0f м",
                AutoVoicePlugin.currentDb, AutoVoicePlugin.currentSmoothedDistance);
            graphics.drawCenteredString(this.font, liveInfo, centerX, 214, 0xCCCCCC);
        }
    }

    private void drawMarker(GuiGraphics graphics, int barX, int barY, int barWidth, double dbVal, int color) {
        double norm = Mth.clamp((dbVal + 60.0) / 60.0, 0.0, 1.0);
        int markerX = barX + (int) (barWidth * norm);
        graphics.fill(markerX - 1, barY - 2, markerX + 1, barY + 14, color);
    }

    @Override
    public void onClose() {
        VoiceConfig.CLIENT_SPEC.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private static class DbSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final Consumer<Double> onChange;

        public DbSlider(int x, int y, int width, int height, String label, double initialDb, double min, double max, Consumer<Double> onChange) {
            super(x, y, width, height, Component.empty(), Mth.clamp((initialDb - min) / (max - min), 0.0, 1.0));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            this.updateMessage();
        }

        public void setDbValue(double db) {
            this.value = Mth.clamp((db - min) / (max - min), 0.0, 1.0);
            this.updateMessage();
        }

        private double getDbValue() {
            return Math.round(min + this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(String.format("%s: %.0f дБ", label, getDbValue())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(getDbValue());
        }
    }
}
