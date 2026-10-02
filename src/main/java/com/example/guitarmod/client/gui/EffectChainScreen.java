package com.example.guitarmod.client.gui;

import com.example.guitarmod.audio.AudioCaptureManager;
import com.example.guitarmod.audio.EffectChain;
import com.example.guitarmod.audio.effects.EffectRegistry;
import com.example.guitarmod.audio.effects.IEffect;
import com.example.guitarmod.data.EffectChainData;
import com.example.guitarmod.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 效果链编辑界面：增删、排序、启停、参数滑条；关闭时保存到吉他物品的 DataComponent。
 * 手持吉他右键打开。
 */
public class EffectChainScreen extends Screen {

    private static final int MAX_VISIBLE = 6;

    private final EffectChain chain = new EffectChain();
    private final List<ResourceLocation> addableIds = new ArrayList<>();
    private int addCandidate = 0;
    private int selectedEffect = -1;
    private final List<AbstractSliderButton> paramSliders = new ArrayList<>();

    public EffectChainScreen() {
        super(Component.translatable("screen.guitarmod.effect_chain"));
    }

    @Override
    protected void init() {
        // 从手持吉他读链
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack held = mc.player.getMainHandItem();
            if (held.is(ModItems.GUITAR.get())) {
                EffectChain loaded = EffectChainData.read(held);
                for (IEffect e : loaded.effects()) chain.add(e);
            }
        }
        addableIds.addAll(EffectRegistry.ids());

        refreshChainWidgets();
    }

    private void refreshChainWidgets() {
        clearWidgets();
        paramSliders.clear();

        int cx = this.width / 2;
        int top = 40;
        int rowH = 22;

        List<IEffect> effects = chain.effects();
        for (int i = 0; i < effects.size() && i < MAX_VISIBLE; i++) {
            final int index = i;
            IEffect effect = effects.get(i);

            Button nameBtn = Button.builder(
                            Component.literal((effect.isEnabled() ? "" : "[x] ") + effect.id().getPath()),
                            b -> {
                                selectedEffect = index;
                                refreshChainWidgets();
                            })
                    .bounds(cx - 170, top + i * rowH, 120, 20)
                    .build();
            addRenderableWidget(nameBtn);

            addRenderableWidget(Button.builder(Component.literal(effect.isEnabled() ? "ON" : "OFF"),
                            b -> {
                                effect.setEnabled(!effect.isEnabled());
                                refreshChainWidgets();
                            })
                    .bounds(cx - 46, top + i * rowH, 36, 20).build());

            addRenderableWidget(Button.builder(Component.literal("↑"),
                            b -> { chain.move(index, -1); fixSelection(index, -1); refreshChainWidgets(); })
                    .bounds(cx - 6, top + i * rowH, 20, 20).build());

            addRenderableWidget(Button.builder(Component.literal("↓"),
                            b -> { chain.move(index, 1); fixSelection(index, 1); refreshChainWidgets(); })
                    .bounds(cx + 18, top + i * rowH, 20, 20).build());

            addRenderableWidget(Button.builder(Component.literal("✕"),
                            b -> {
                                chain.remove(index);
                                if (selectedEffect == index) selectedEffect = -1;
                                else if (selectedEffect > index) selectedEffect--;
                                refreshChainWidgets();
                            })
                    .bounds(cx + 42, top + i * rowH, 20, 20).build());
        }

        // 添加效果
        int addY = top + Math.min(effects.size(), MAX_VISIBLE) * rowH + 4;
        if (!addableIds.isEmpty()) {
            String cand = addableIds.get(addCandidate).getPath();
            addRenderableWidget(Button.builder(Component.literal("选择: " + cand),
                            b -> {
                                addCandidate = (addCandidate + 1) % addableIds.size();
                                refreshChainWidgets();
                            })
                    .bounds(cx - 170, addY, 120, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.guitarmod.add_effect"),
                            b -> {
                                IEffect created = EffectRegistry.create(addableIds.get(addCandidate));
                                if (created != null) {
                                    chain.add(created);
                                }
                                refreshChainWidgets();
                            })
                    .bounds(cx - 46, addY, 108, 20).build());
        }

        // 选中效果的参数滑条（右栏）
        if (selectedEffect >= 0 && selectedEffect < effects.size()) {
            IEffect effect = effects.get(selectedEffect);
            int sliderY = top;
            for (Map.Entry<String, Float> entry : effect.getParams().entrySet()) {
                String key = entry.getKey();
                float value = entry.getValue();
                ParamSlider slider = new ParamSlider(cx + 80, sliderY, 160, 20, effect, key, value);
                paramSliders.add(slider);
                addRenderableWidget(slider);
                sliderY += 24;
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 60, this.height - 40, 120, 20).build());
    }

    private void fixSelection(int index, int delta) {
        if (selectedEffect == index) {
            selectedEffect = index + delta;
        }
    }

    @Override
    public void onClose() {
        // 保存到物品组件，并热更新到采集线程
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack held = mc.player.getMainHandItem();
            if (held.is(ModItems.GUITAR.get())) {
                EffectChainData.write(held, chain);
                AudioCaptureManager.INSTANCE.setChain(chain);
            }
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        if (selectedEffect >= 0 && selectedEffect < chain.effects().size()) {
            IEffect e = chain.effects().get(selectedEffect);
            g.drawString(this.font, e.id().toString(), this.width / 2 + 80, 30, 0xFFCC66);
        }
    }

    /** 参数滑条：滑条值 0~1 映射到 0~paramMax(key)。 */
    private static class ParamSlider extends AbstractSliderButton {
        private final IEffect effect;
        private final String key;
        private final float max;

        ParamSlider(int x, int y, int w, int h, IEffect effect, String key, float initial) {
            super(x, y, w, h, Component.empty(), 0.0);
            this.effect = effect;
            this.key = key;
            this.max = Math.max(0.001f, effect.paramMax(key));
            this.value = Math.max(0.0, Math.min(1.0, initial / this.max));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(key + ": " + String.format("%.2f", this.value * this.max)));
        }

        @Override
        protected void applyValue() {
            effect.setParam(key, (float) (this.value * this.max));
        }
    }
}
