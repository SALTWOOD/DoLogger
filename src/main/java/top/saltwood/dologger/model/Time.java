package top.saltwood.dologger.model;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import top.saltwood.dologger.util.LanguageResolver;

import java.util.Date;
import java.util.Locale;

public record Time(long time) {

    public MutableComponent getFormattedTimeAgo() {
        long timeAgo = Math.max(0L, System.currentTimeMillis() - time);
        if (timeAgo < TimeUnit.HOUR.getMilliseconds() / 2) {
            return getTimeAgoComponent((double) timeAgo / TimeUnit.MINUTE.getMilliseconds(), TimeUnit.MINUTE.getName());
        }
        if (timeAgo < TimeUnit.DAY.getMilliseconds() / 2) {
            return getTimeAgoComponent((double) timeAgo / TimeUnit.HOUR.getMilliseconds(), TimeUnit.HOUR.getName());
        }
        if (timeAgo < TimeUnit.YEAR.getMilliseconds() / 2) {
            return getTimeAgoComponent((double) timeAgo / TimeUnit.DAY.getMilliseconds(), TimeUnit.DAY.getName());
        }
        return getTimeAgoComponent((double) timeAgo / TimeUnit.YEAR.getMilliseconds(), TimeUnit.YEAR.getName());
    }

    private MutableComponent getTimeAgoComponent(double timeAgo, Component unit) {
        return LanguageResolver.component("dologger.lookup.time.ago", String.format(Locale.ROOT, "%.2f", timeAgo), LanguageResolver.resolve("dologger.time.divider"), unit)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(new Date(time).toString()).withStyle(ChatFormatting.GRAY))));
    }
}
