package com.example.guitarmod.audio;

import javax.sound.sampled.Mixer;

/**
 * 一个可用的录音输入设备描述，供 GUI 选择。
 */
public record AudioDeviceInfo(String name, Mixer.Info mixerInfo) {

    @Override
    public String toString() {
        return name;
    }
}
