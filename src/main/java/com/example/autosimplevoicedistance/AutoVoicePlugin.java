package com.example.autosimplevoicedistance;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import com.example.autosimplevoicedistance.network.VolumePayload;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ForgeVoicechatPlugin
public class AutoVoicePlugin implements VoicechatPlugin {

    public static final Map<UUID, Float> PLAYER_DISTANCES = new ConcurrentHashMap<>();

    public static volatile double currentDb = -60.0;
    public static volatile float currentSmoothedDistance = 24.0f;

    private long lastPacketSentTime = 0L;
    private long holdPeakUntil = 0L;

    @Override
    public String getPluginId() {
        return "autosimplevoicedistance";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
        registration.registerEvent(EntitySoundPacketEvent.class, this::onServerEntitySound);
    }

    private void onClientSound(ClientSoundEvent event) {
        short[] rawAudio = event.getRawAudio();
        if (rawAudio == null || rawAudio.length == 0) return;

        double sumSquares = 0.0;
        for (short sample : rawAudio) {
            sumSquares += sample * sample;
        }
        double rms = Math.sqrt(sumSquares / rawAudio.length);
        double db = (rms > 0) ? 20.0 * Math.log10(rms / 32768.0) : -60.0;
        currentDb = Mth.clamp(db, -60.0, 0.0);

        if (!VoiceConfig.CLIENT.enabled.get()) {
            currentSmoothedDistance = VoiceConfig.CLIENT.normalDistance.get().floatValue();
            sendDistancePacket(currentSmoothedDistance);
            return;
        }

        double wDb = VoiceConfig.CLIENT.whisperDb.get();
        double nDb = VoiceConfig.CLIENT.normalDb.get();
        double sDb = VoiceConfig.CLIENT.shoutDb.get();

        float wDist = VoiceConfig.CLIENT.whisperDistance.get().floatValue();
        float nDist = VoiceConfig.CLIENT.normalDistance.get().floatValue();
        float sDist = VoiceConfig.CLIENT.shoutDistance.get().floatValue();

        float targetDist;
        if (currentDb <= wDb) {
            targetDist = wDist;
        } else if (currentDb <= nDb) {
            float t = (float) ((currentDb - wDb) / Math.max(1.0, nDb - wDb));
            targetDist = Mth.lerp(t, wDist, nDist);
        } else if (currentDb <= sDb) {
            float t = (float) ((currentDb - nDb) / Math.max(1.0, sDb - nDb));
            targetDist = Mth.lerp(t, nDist, sDist);
        } else {
            targetDist = sDist;
        }

        long now = System.currentTimeMillis();
        if (targetDist >= currentSmoothedDistance) {
            currentSmoothedDistance = Mth.lerp(0.35f, currentSmoothedDistance, targetDist);
            holdPeakUntil = now + 800L;
        } else if (now > holdPeakUntil) {
            currentSmoothedDistance = Mth.lerp(0.03f, currentSmoothedDistance, targetDist);
        }

        sendDistancePacket(currentSmoothedDistance);
    }

    private void sendDistancePacket(float distance) {
        long now = System.currentTimeMillis();
        if (now - lastPacketSentTime >= 100L) {
            lastPacketSentTime = now;
            PacketDistributor.sendToServer(new VolumePayload(distance));
        }
    }

    private void onServerEntitySound(EntitySoundPacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;

        UUID playerUuid = sender.getPlayer().getUuid();
        Float customDistance = PLAYER_DISTANCES.get(playerUuid);

        if (customDistance != null && event.getVoicechat() instanceof VoicechatServerApi serverApi) {
            event.cancel();
            LocationalSoundPacket customPacket = event.getPacket()
                    .locationalSoundPacketBuilder()
                    .position(sender.getPlayer().getPosition())
                    .distance(customDistance)
                    .build();

            for (var player : serverApi.getPlayersInRange(sender.getPlayer().getServerLevel(), sender.getPlayer().getPosition(), customDistance)) {
                if (!player.getUuid().equals(playerUuid)) {
                    VoicechatConnection receiver = serverApi.getConnectionOf(player.getUuid());
                    if (receiver != null) {
                        serverApi.sendLocationalSoundPacketTo(receiver, customPacket);
                    }
                }
            }
        }
    }


}
