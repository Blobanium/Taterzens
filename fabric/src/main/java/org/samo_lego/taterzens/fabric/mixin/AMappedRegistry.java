package org.samo_lego.taterzens.fabric.mixin;

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleRegistry.class)
public interface AMappedRegistry<T> {
    @Accessor("rawIdToEntry")
    ObjectList<RegistryEntry.Reference<T>> getById();
}
