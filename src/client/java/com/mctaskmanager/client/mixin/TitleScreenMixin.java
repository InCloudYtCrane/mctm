package com.mctaskmanager.client.mixin;

import com.mctaskmanager.client.SessionHistoryScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void mctm$addButton(CallbackInfo ci) {
        TitleScreen self = (TitleScreen) (Object) this;

        // "多人游戏"按钮的翻译文本（跟着语言走）
        String multiText = Component.translatable("menu.multiplayer").getString();

        // 收集所有 widget
        List<AbstractWidget> all = new ArrayList<>();
        for (var child : self.children()) {
            if (child instanceof AbstractWidget w) {
                all.add(w);
            }
        }

        // 找"多人游戏"按钮
        AbstractWidget multiplayer = null;
        for (AbstractWidget w : all) {
            if (w instanceof Button btn) {
                if (btn.getMessage().getString().equals(multiText)) {
                    multiplayer = w;
                    break;
                }
            }
        }

        if (multiplayer == null) {
            // 兜底：左下角
            Button btn = Button.builder(
                    Component.translatable("mctm.session.open"),
                    b -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(new SessionHistoryScreen(self));
                        }
                    }
            ).bounds(8, this.height - 30, 140, 20).build();
            this.addRenderableWidget(btn);
            return;
        }

        int multiBottom = multiplayer.getY() + multiplayer.getHeight();
        int shift = multiplayer.getHeight() + 4; // 24，往下挪一格

        // 把"多人游戏"下方的所有按钮往下挪
        for (AbstractWidget w : all) {
            if (w.getY() >= multiBottom) {
                w.setY(w.getY() + shift);
            }
        }

        // 在"多人游戏"下方插入我们的按钮
        Button newBtn = Button.builder(
                Component.translatable("mctm.session.open"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new SessionHistoryScreen(self));
                    }
                }
        ).bounds(multiplayer.getX(), multiBottom + 4,
                multiplayer.getWidth(), multiplayer.getHeight()).build();

        this.addRenderableWidget(newBtn);
    }
}