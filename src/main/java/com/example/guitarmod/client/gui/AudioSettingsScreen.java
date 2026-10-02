package com.example.guitarmod.client.gui;

import com.example.guitarmod.audio.AudioCaptureManager;
import com.example.guitarmod.audio.AudioDeviceInfo;
import com.example.guitarmod.config.GuitarConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 音频设置界面：选择输入设备、启动/停止采集测试、实时电平表、阈值滑条。
 * 默认按 G 键打开。
 */
public class AudioSettingsScreen extends Screen {

    private static final int METER_WIDTH = 200;

    /** devices.get(0) 恒为"系统默认"。 */
    private final List<String> deviceNames = new ArrayList<>();
    private final List<AudioDeviceInfo> devices = new ArrayList<>();
    private int selected = 0;

    private Button deviceButton;
    private Button toggleButton;

    public AudioSettingsScreen() {
        super(Component.translatable("screen.guitarmod.audio_settings"));
    }

    @Override
    protected void init() {
        refreshDevices();

        int cx = this.width / 2;
        int top = this.height / 2 - 60;

        deviceButton = Button.builder(Component.empty(), b -> {
                    selected = (selected + 1) % deviceNames.size();
                    updateLabels();
                })
                .bounds(cx - 110, top, 220, 20)
                .build();
        addRenderableWidget(deviceButton);

        toggleButton = Button.builder(Component.empty(), b -> {
                    AudioCaptureManager cap = AudioCaptureManager.INSTANCE;
                    if (cap.isRunning()) {
                        cap.stop();
                    } else {
                        String name = selected == 0 ? "" : deviceNames.get(selected);
                        cap.start(name);
                    }
                    updateLabels();
                })
                .bounds(cx - 110, top + 26, 220, 20)
                .build();
        addRenderableWidget(toggleButton);

        addRenderableWidget(new AbstractSliderButton(cx - 110, top + 52, 220, 20,
                Component.empty(), GuitarConfig.LEVEL_THRESHOLD.get()) {
            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("screen.guitarmod.threshold",
                        String.format("%.3f", this.value)));
            }

            @Override
            protected void applyValue() {
                GuitarConfig.LEVEL_THRESHOLD.set(this.value);
            }
        });

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 110, top + 96, 220, 20)
                .build());

        updateLabels();
    }

    private void refreshDevices() {
        deviceNames.clear();
        devices.clear();
        deviceNames.add(Component.translatable("screen.guitarmod.default_device").getString());
        try {
            for (AudioDeviceInfo info : AudioCaptureManager.INSTANCE.listDevices()) {
                deviceNames.add(info.name());
                devices.add(info);
            }
        } catch (Throwable ignored) {
        }
        // 回选配置里的设备
        String configured = GuitarConfig.DEVICE_NAME.get();
        selected = 0;
        if (configured != null && !configured.isEmpty()) {
            for (int i = 1; i < deviceNames.size(); i++) {
                if (deviceNames.get(i).contains(configured)) {
                    selected = i;
                    break;
                }
            }
        }
    }

    private void updateLabels() {
        String name = deviceNames.isEmpty() ? "?" : deviceNames.get(selected);
        deviceButton.setMessage(Component.translatable("screen.guitarmod.device", name));
        toggleButton.setMessage(AudioCaptureManager.INSTANCE.isRunning()
                ? Component.translatable("screen.guitarmod.stop_capture")
                : Component.translatable("screen.guitarmod.start_capture"));
    }

    @Override
    public void onClose() {
        // 保存设备选择到配置
        GuitarConfig.DEVICE_NAME.set(selected == 0 ? "" : deviceNames.get(selected));
        super.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int top = this.height / 2 - 60;

        g.drawCenteredString(this.font, this.title, cx, top - 18, 0xFFFFFF);

        // 电平表
        int meterY = top + 78;
        float meter = AudioCaptureManager.INSTANCE.meter();
        g.fill(cx - METER_WIDTH / 2, meterY, cx + METER_WIDTH / 2, meterY + 8, 0xFF202020);
        int fillW = Math.min(METER_WIDTH, (int) (meter * METER_WIDTH * 2));
        int color = meter > GuitarConfig.LEVEL_THRESHOLD.get() ? 0xFFFF4444 : 0xFF44CC44;
        g.fill(cx - METER_WIDTH / 2, meterY, cx - METER_WIDTH / 2 + fillW, meterY + 8, color);

        AudioCaptureManager cap = AudioCaptureManager.INSTANCE;
        String status = cap.isRunning()
                ? Component.translatable("screen.guitarmod.status_running", cap.activeDeviceName()).getString()
                : Component.translatable("screen.guitarmod.status_stopped").getString();
        if (cap.error() != null) {
            status = Component.translatable("screen.guitarmod.status_error", cap.error()).getString();
        }
        g.drawCenteredString(this.font, status, cx, top + 122, 0xAAAAAA);
    }
}
