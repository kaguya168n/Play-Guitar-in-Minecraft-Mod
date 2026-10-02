package com.example.guitarmod.audio;

import com.example.guitarmod.GuitarMod;
import com.example.guitarmod.config.GuitarConfig;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 音频采集层（独立守护线程，javax.sound.sampled）。
 * 采集 -> 效果链 -> 有界队列（满了丢最旧）-> AlStreamPlayer 消费。
 * 本类绝不触碰 Minecraft 游戏对象，只通过原子量/队列与游戏侧交换数据。
 */
public final class AudioCaptureManager {

    public static final AudioCaptureManager INSTANCE = new AudioCaptureManager();

    private static final int QUEUE_CAPACITY = 64; // 每块约 10ms，约 640ms 缓冲上限

    private final ArrayBlockingQueue<float[]> playbackQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile float meter = 0f;
    private volatile String error = null;
    private volatile EffectChain chain = new EffectChain();
    private volatile String activeDeviceName = "";

    private Thread thread;
    private TargetDataLine dataLine;

    private AudioCaptureManager() {}

    /** 当前使用的效果链（热替换安全：写引用是原子的）。 */
    public EffectChain chain() {
        return chain;
    }

    public void setChain(EffectChain chain) {
        this.chain = chain;
    }

    public float meter() {
        return meter;
    }

    public String error() {
        return error;
    }

    public boolean isRunning() {
        return running.get();
    }

    public String activeDeviceName() {
        return activeDeviceName;
    }

    /** 消费端（AlStreamPlayer）取一块处理后的 PCM；无数据返回 null。 */
    public float[] pollBlock() {
        return playbackQueue.poll();
    }

    /** 暂停/停止时丢弃队列里的残余数据。 */
    public void flushQueue() {
        playbackQueue.clear();
        meter = 0f;
    }

    /** 枚举系统里所有支持目标格式（48k/16bit/单声道）的录音设备。 */
    public List<AudioDeviceInfo> listDevices() {
        int sampleRate = GuitarConfig.SAMPLE_RATE.get();
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info wanted = new DataLine.Info(TargetDataLine.class, format);
        List<AudioDeviceInfo> result = new ArrayList<>();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.isLineSupported(wanted)) {
                    result.add(new AudioDeviceInfo(info.getName(), info));
                }
            } catch (Throwable t) {
                GuitarMod.LOGGER.debug("[GuitarMod] skip mixer {}: {}", info.getName(), t.toString());
            }
        }
        return result;
    }

    /** 用配置里的设备名（子串匹配）启动采集；空串 = 系统默认设备。 */
    public synchronized boolean start() {
        String preferred = GuitarConfig.DEVICE_NAME.get();
        return start(preferred == null ? "" : preferred);
    }

    /** 用指定设备名（子串匹配，空串 = 默认设备）启动采集。 */
    public synchronized boolean start(String deviceNameSubstring) {
        if (running.get()) return true;
        error = null;

        int sampleRate = GuitarConfig.SAMPLE_RATE.get();
        int bufferFrames = GuitarConfig.BUFFER_FRAMES.get();
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info wanted = new DataLine.Info(TargetDataLine.class, format);

        try {
            TargetDataLine line = null;
            String resolvedName = "系统默认";
            if (deviceNameSubstring == null || deviceNameSubstring.isEmpty()) {
                if (AudioSystem.isLineSupported(wanted)) {
                    line = (TargetDataLine) AudioSystem.getLine(wanted);
                }
            } else {
                for (AudioDeviceInfo dev : listDevices()) {
                    if (dev.name().contains(deviceNameSubstring)) {
                        line = (TargetDataLine) AudioSystem.getMixer(dev.mixerInfo()).getLine(wanted);
                        resolvedName = dev.name();
                        break;
                    }
                }
                if (line == null) {
                    error = "找不到输入设备: " + deviceNameSubstring;
                    GuitarMod.LOGGER.warn("[GuitarMod] {}", error);
                    return false;
                }
            }
            if (line == null) {
                error = "系统默认录音设备不可用";
                GuitarMod.LOGGER.warn("[GuitarMod] {}", error);
                return false;
            }

            line.open(format, bufferFrames * 2 * 4); // 4 块硬件缓冲
            line.start();
            this.dataLine = line;
            this.activeDeviceName = resolvedName;
            running.set(true);

            thread = new Thread(this::captureLoop, "guitarmod-audio-capture");
            thread.setDaemon(true);
            thread.start();
            GuitarMod.LOGGER.info("[GuitarMod] capture started on [{}], {}Hz, {} frames/block",
                    resolvedName, sampleRate, bufferFrames);
            return true;
        } catch (Throwable t) {
            error = "打开录音设备失败: " + t.getMessage();
            GuitarMod.LOGGER.warn("[GuitarMod] open capture device failed", t);
            running.set(false);
            return false;
        }
    }

    public synchronized void stop() {
        running.set(false);
        if (thread != null) {
            try {
                thread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
        if (dataLine != null) {
            try {
                dataLine.stop();
                dataLine.close();
            } catch (Throwable ignored) {
            }
            dataLine = null;
        }
        flushQueue();
        GuitarMod.LOGGER.info("[GuitarMod] capture stopped");
    }

    /** 采集主循环：阻塞读天然限速；严禁在此线程调用 Minecraft 对象。 */
    private void captureLoop() {
        int sampleRate = GuitarConfig.SAMPLE_RATE.get();
        int bufferFrames = GuitarConfig.BUFFER_FRAMES.get();
        byte[] raw = new byte[bufferFrames * 2]; // 16bit mono

        while (running.get()) {
            TargetDataLine line = this.dataLine;
            if (line == null) break;
            int n;
            try {
                n = line.read(raw, 0, raw.length);
            } catch (Throwable t) {
                GuitarMod.LOGGER.warn("[GuitarMod] capture read failed", t);
                break;
            }
            if (n <= 0) continue;

            int frames = n / 2;
            float[] pcm = new float[frames];
            double sumSq = 0;
            for (int i = 0; i < frames; i++) {
                int lo = raw[i * 2] & 0xFF;
                int hi = raw[i * 2 + 1]; // little-endian, signed high byte
                short s = (short) ((hi << 8) | lo);
                float v = s / 32768f;
                pcm[i] = v;
                sumSq += (double) v * v;
            }

            chain.process(pcm, frames, sampleRate);

            // RMS 电平 -> 动画触发 / GUI 电平表
            meter = frames > 0 ? (float) Math.sqrt(sumSq / frames) : 0f;

            if (!playbackQueue.offer(pcm)) {
                playbackQueue.poll(); // 满了丢最旧，保延迟
                playbackQueue.offer(pcm);
            }
        }
        running.set(false);
    }
}
