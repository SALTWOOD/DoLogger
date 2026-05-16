package top.saltwood.dologger.model;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import top.saltwood.dologger.util.LanguageResolver;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record Time(long time) {

    private static final DateTimeFormatter ABSOLUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd, HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public MutableComponent getFormattedTimeAgo() {
        long timeAgo = Math.max(0L, System.currentTimeMillis() - time);
        if (timeAgo < TimeUnit.MINUTE.getMilliseconds()) {
            long amount = amount(timeAgo, TimeUnit.SECOND);
            return getTimeAgoComponent(amount, TimeUnit.SECOND.getName(amount));
        }
        if (timeAgo < TimeUnit.HOUR.getMilliseconds()) {
            long amount = amount(timeAgo, TimeUnit.MINUTE);
            return getTimeAgoComponent(amount, TimeUnit.MINUTE.getName(amount));
        }
        if (timeAgo < TimeUnit.DAY.getMilliseconds()) {
            long amount = amount(timeAgo, TimeUnit.HOUR);
            return getTimeAgoComponent(amount, TimeUnit.HOUR.getName(amount));
        }
        if (timeAgo < TimeUnit.YEAR.getMilliseconds()) {
            long amount = amount(timeAgo, TimeUnit.DAY);
            return getTimeAgoComponent(amount, TimeUnit.DAY.getName(amount));
        }
        long amount = amount(timeAgo, TimeUnit.YEAR);
        return getTimeAgoComponent(amount, TimeUnit.YEAR.getName(amount));
    }

    private static long amount(long timeAgo, TimeUnit unit) {
        return Math.max(1L, timeAgo / unit.getMilliseconds());
    }

    private MutableComponent getTimeAgoComponent(long timeAgo, Component unit) {
        return LanguageResolver.component("dologger.lookup.time.ago", Long.toString(timeAgo), unit)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(ABSOLUTE_FORMAT.format(Instant.ofEpochMilli(time))).withStyle(ChatFormatting.GRAY))));
    }
}
