package top.saltwood.dologger.model;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class SimpleItemStack {

    private final Item item;
    private final int count;
    private final @Nullable DataComponentPatch components;

    public SimpleItemStack(ItemStack itemStack) {
        this(itemStack.getItem(), itemStack.getCount(), itemStack.getComponentsPatch());
    }

    public SimpleItemStack(String itemLocation, int count, @Nullable DataComponentPatch components) {
        this(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemLocation)), count, components);
    }

    public SimpleItemStack(Item item, int count, @Nullable DataComponentPatch components) {
        this.item = item;
        this.count = count;
        this.components = components;
    }

    public static SimpleItemStack of(ItemStack stack) {
        return new SimpleItemStack(stack);
    }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public @Nullable DataComponentPatch getComponents() {
        return components;
    }

    public byte @Nullable [] getTagBytes() {
        if (components == null || components.isEmpty()) {
            return null;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DynamicOps<Tag> ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag encoded = ItemStack.OPTIONAL_CODEC.encodeStart(ops, toItemStack()).getOrThrow(IllegalStateException::new);
            CompoundTag compoundTag = encoded instanceof CompoundTag tag ? tag : new CompoundTag();
            NbtIo.writeCompressed(compoundTag, outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            return null;
        }
    }

    public ItemStack toItemStack() {
        ItemStack itemStack = new ItemStack(item == null ? Items.AIR : item, count);
        if (components != null && !components.isEmpty()) {
            itemStack.applyComponents(components);
        }
        return itemStack;
    }

    public boolean isEmpty() {
        return item == null || item == Items.AIR;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        SimpleItemStack that = (SimpleItemStack) object;
        return Objects.equals(item, that.item) && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, components);
    }
}
