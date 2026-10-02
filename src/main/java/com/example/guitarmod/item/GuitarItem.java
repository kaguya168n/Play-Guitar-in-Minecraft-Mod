package com.example.guitarmod.item;

import com.example.guitarmod.client.ClientScreenOpener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * 电吉他物品。主手持有时客户端开始采集并播放音频；
 * 右键打开效果链编辑界面。
 */
public class GuitarItem extends Item {

    public GuitarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT) {
            ClientScreenOpener.openEffectChainScreen();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
