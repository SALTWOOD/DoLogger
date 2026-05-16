package top.saltwood.dologger.model;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class User {

    private final String name;
    private final @Nullable UUID uuid;

    public User(String name, @Nullable UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public Optional<UUID> getUuid() {
        return Optional.ofNullable(uuid);
    }

    public Component getNameComponent() {
        return Component.literal(name).withStyle(ChatFormatting.GRAY);
    }
}
