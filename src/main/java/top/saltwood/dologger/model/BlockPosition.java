package top.saltwood.dologger.model;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

public record BlockPosition(int x, int y, int z) {

    public Component getComponent() {
        return Component.translatable("dologger.lookup.position", x, y, z)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("dologger.lookup.position.teleport")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + x + " " + y + " " + z)));
    }
}
