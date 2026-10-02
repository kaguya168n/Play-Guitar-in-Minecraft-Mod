package com.example.guitarmod.audio.playback;

import com.example.guitarmod.GuitarMod;
import com.example.guitarmod.audio.AudioCaptureManager;
import com.example.guitarmod.config.GuitarConfig;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;

import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.AL_BUFFERS_PROCESSED;
import static org.lwjgl.openal.AL10.AL_FALSE;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.AL_LOOPING;
import static org.lwjgl.openal.AL10.AL_NONE;
import static org.lwjgl.openal.AL10.AL_PLAYING;
import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.AL_REFERENCE_DISTANCE;
import static org.lwjgl.openal.AL10.AL_SOURCE_STATE;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alDeleteSources;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.AL10.alGenSources;
import static org.lwjgl.openal.AL10.alGetError;
import static org.lwjgl.openal.AL10.alGetSourcei;
import static org.lwjgl.openal.AL10.alSource3f;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourceQueueBuffers;
import static org.lwjgl.openal.AL10.alSourceStop;
import static org.lwjgl.openal.AL10.alSourceUnqueueBuffers;
import static org.lwjgl.openal.AL10.alSourcef;
import static org.lwjgl.openal.AL10.alSourcei;

/**
 * OpenAL 流式播放器：自建 source + 6 个 buffer 轮转，复用游戏已有的 OpenAL context。
 * 只在客户端 tick 线程调用；关闭时必须删除 source/buffer，否则泄漏。
 */
public final class AlStreamPlayer {

    private static final int BUFFER_COUNT = 6;

    private int source = AL_NONE;
    private int[] buffers;
    private ShortBuffer scratch;
    private boolean initialized = false;
    private long lastErrorLog = 0;

    public boolean isInitialized() {
        return initialized;
    }

    /** 绑定到游戏现有 OpenAL context（游戏已持有 context，模组不能再创建）。 */
    public synchronized boolean init() {
        if (initialized) return true;
        try {
            long context = ALC10.alcGetCurrentContext();
            if (context == 0L) {
                GuitarMod.LOGGER.warn("[GuitarMod] no current OpenAL context yet");
                return false;
            }
            long device = ALC10.alcGetContextsDevice(context);
            AL.createCapabilities(ALC.createCapabilities(device));

            source = alGenSources();
            buffers = new int[BUFFER_COUNT];
            for (int i = 0; i < BUFFER_COUNT; i++) {
                buffers[i] = alGenBuffers();
            }
            scratch = BufferUtils.createShortBuffer(GuitarConfig.BUFFER_FRAMES.get());

            alSourcei(source, AL_LOOPING, AL_FALSE);
            alSourcef(source, AL_REFERENCE_DISTANCE, 4.0f);
            checkError("init");
            initialized = true;
            GuitarMod.LOGGER.info("[GuitarMod] OpenAL stream player initialized");
            return true;
        } catch (Throwable t) {
            GuitarMod.LOGGER.warn("[GuitarMod] OpenAL init failed", t);
            close();
            return false;
        }
    }

    /**
     * 每客户端 tick 调用：把采集队列里的新块填入已播放完的 buffer，位置跟随玩家。
     */
    public synchronized void update(Vec3 pos, float volume) {
        if (!initialized) return;
        alSource3f(source, AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
        alSourcef(source, AL_GAIN, volume);

        int sampleRate = GuitarConfig.SAMPLE_RATE.get();
        int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buf = alSourceUnqueueBuffers(source);
            float[] next = AudioCaptureManager.INSTANCE.pollBlock();
            if (next != null && next.length > 0) {
                fillBuffer(buf, next, sampleRate);
                alSourceQueueBuffers(source, buf);
            }
        }

        // 冷启动 / 断流恢复：源没在播且有数据时补一块直接开播
        if (alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING) {
            float[] next = AudioCaptureManager.INSTANCE.pollBlock();
            if (next != null && next.length > 0) {
                int buf = alSourceUnqueueBuffers(source);
                if (buf == AL_NONE && buffers != null) buf = buffers[0];
                fillBuffer(buf, next, sampleRate);
                alSourceQueueBuffers(source, buf);
                alSourcePlay(source);
            }
        }
        checkError("update");
    }

    public synchronized void stopPlayback() {
        if (!initialized) return;
        alSourceStop(source);
        // 清空排队 buffer，避免恢复时播放旧数据
        int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            alSourceUnqueueBuffers(source);
        }
    }

    public synchronized void close() {
        if (source != AL_NONE) {
            try {
                alSourceStop(source);
                alDeleteSources(source);
            } catch (Throwable ignored) {
            }
            source = AL_NONE;
        }
        if (buffers != null) {
            try {
                alDeleteBuffers(buffers);
            } catch (Throwable ignored) {
            }
            buffers = null;
        }
        scratch = null;
        initialized = false;
        GuitarMod.LOGGER.info("[GuitarMod] OpenAL stream player closed");
    }

    private void fillBuffer(int buf, float[] pcm, int sampleRate) {
        if (scratch == null || scratch.capacity() < pcm.length) {
            scratch = BufferUtils.createShortBuffer(pcm.length);
        }
        scratch.clear();
        for (float v : pcm) {
            float c = Math.max(-1f, Math.min(1f, v));
            scratch.put((short) (c * 32767f));
        }
        scratch.flip();
        alBufferData(buf, AL_FORMAT_MONO16, scratch, sampleRate);
    }

    private void checkError(String where) {
        int err = alGetError();
        if (err != AL10Error.NONE) {
            long now = System.currentTimeMillis();
            if (now - lastErrorLog > 5000) {
                lastErrorLog = now;
                GuitarMod.LOGGER.warn("[GuitarMod] OpenAL error 0x{} at {}", Integer.toHexString(err), where);
            }
        }
    }

    /** alGetError 常量别名，避免引入多余静态导入冲突。 */
    private static final class AL10Error {
        static final int NONE = 0;
    }
}
