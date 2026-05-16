package top.saltwood.dologger.model.history;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.Time;
import top.saltwood.dologger.model.User;
import top.saltwood.dologger.model.action.BlockAction;

import java.util.UUID;

public class BlockHistory extends History {

    private final String material;
    private final int id;
    private final @Nullable Long revertedAt;
    private final @Nullable Long restoredAt;

    public BlockHistory(long time, String name, @Nullable String uuid, int x, int y, int z, String material, int blockAction) {
        this(new Time(time), new User(name, uuid == null ? null : UUID.fromString(uuid)), new BlockPosition(x, y, z), material, BlockAction.fromId(blockAction));
    }

    public BlockHistory(Time time, User user, BlockPosition position, String material, BlockAction action) {
        this(0, time, user, position, material, action, null, null);
    }

    public BlockHistory(int id, Time time, User user, BlockPosition position, String material, BlockAction action, @Nullable Long revertedAt, @Nullable Long restoredAt) {
        super(time, user, position, action);
        this.id = id;
        this.material = material;
        this.revertedAt = revertedAt;
        this.restoredAt = restoredAt;
    }

    public int getId() {
        return id;
    }

    public String getMaterial() {
        return material;
    }

    public @Nullable Long getRevertedAt() {
        return revertedAt;
    }

    public @Nullable Long getRestoredAt() {
        return restoredAt;
    }

    public boolean isReverted() {
        return revertedAt != null;
    }

    public boolean isRestored() {
        return restoredAt != null;
    }

    public boolean isCurrentlyReverted() {
        return isReverted() && !isRestored();
    }

    @Override
    public Component getComponent() {
        MutableComponent component = getTime().getFormattedTimeAgo().append(" ")
                .append(getAction().getPrefix())
                .append(getUser().getNameComponent()).append(" ")
                .append(getAction().getPastTense()).append(" ")
                .append(getMaterialComponent());
        return isCurrentlyReverted() ? component.withStyle(ChatFormatting.STRIKETHROUGH) : component;
    }

    @Override
    public Component getMaterialComponent() {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(material));
        Item item = block == null ? Items.AIR : block.asItem();
        MutableComponent blockName = block != null && block != net.minecraft.world.level.block.Blocks.AIR
                ? block.getName().copy()
                : Component.literal(material.replace("minecraft:", ""));

        if (item != null && item != Items.AIR) {
            return Component.empty()
                    .append(blockName)
                    .withStyle(blockName.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(new ItemStack(item)))));
        }

        return Component.empty()
                .append(blockName)
                .withStyle(blockName.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(material))));
    }
}
