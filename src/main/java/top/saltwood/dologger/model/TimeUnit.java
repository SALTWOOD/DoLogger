package top.saltwood.dologger.model;

import net.minecraft.network.chat.Component;
import top.saltwood.dologger.util.LanguageResolver;

public enum TimeUnit {
    MINUTE(60_000L),
    HOUR(3_600_000L),
    DAY(86_400_000L),
    YEAR(31_536_000_000L);

    private final long milliseconds;

    TimeUnit(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    public Component getName() {
        return LanguageResolver.component("dologger.time.unit." + name().toLowerCase());
    }
}
