package com.example.autosimplevoicedistance;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VoiceConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CLIENT = new Client(builder);
        CLIENT_SPEC = builder.build();
    }

    public static class Client {
        public final ModConfigSpec.DoubleValue whisperDb;
        public final ModConfigSpec.DoubleValue normalDb;
        public final ModConfigSpec.DoubleValue shoutDb;

        public final ModConfigSpec.DoubleValue whisperDistance;
        public final ModConfigSpec.DoubleValue normalDistance;
        public final ModConfigSpec.DoubleValue shoutDistance;

        public final ModConfigSpec.BooleanValue showHud;
        public final ModConfigSpec.BooleanValue enabled;

        public Client(ModConfigSpec.Builder builder) {
            builder.push("calibration");
            whisperDb = builder.comment("Порог тихого шёпота (дБ)").defineInRange("whisperDb", -48.0, -60.0, 0.0);
            normalDb = builder.comment("Уровень обычной речи (дБ)").defineInRange("normalDb", -32.0, -60.0, 0.0);
            shoutDb = builder.comment("Порог крика (дБ)").defineInRange("shoutDb", -16.0, -60.0, 0.0);
            builder.pop();

            builder.push("distances");
            whisperDistance = builder.comment("Радиус шёпота в блоках").defineInRange("whisperDistance", 5.0, 2.0, 20.0);
            normalDistance = builder.comment("Радиус обычной речи в блоках").defineInRange("normalDistance", 24.0, 10.0, 48.0);
            shoutDistance = builder.comment("Радиус крика в блоках").defineInRange("shoutDistance", 64.0, 32.0, 100.0);
            builder.pop();

            builder.push("hud");
            enabled = builder.comment("Включить автоматический контроль радиуса").define("enabled", true);
            showHud = builder.comment("Показывать индикатор радиуса на экране").define("showHud", true);
            builder.pop();
        }
    }
}
