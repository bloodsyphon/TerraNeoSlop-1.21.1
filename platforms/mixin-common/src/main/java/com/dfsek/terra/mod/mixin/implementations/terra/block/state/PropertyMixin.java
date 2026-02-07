package com.dfsek.terra.mod.mixin.implementations.terra.block.state;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Interface.Remap;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.dfsek.terra.api.block.state.properties.Property;


@Mixin(net.minecraft.state.property.Property.class)
@Implements(@Interface(iface = Property.class, prefix = "terra$", remap = Remap.NONE))
public abstract class PropertyMixin<T> {
    @Intrinsic
    public Collection<T> terra$values() {
        return valuesCompat();
    }

    @Intrinsic
    public Class<T> terra$getType() {
        return typeCompat();
    }

    @Intrinsic
    public String terra$getID() {
        return nameCompat();
    }

    @Unique
    @SuppressWarnings("unchecked")
    private List<T> valuesCompat() {
        try {
            Method getPossibleValues = getClass().getMethod("getPossibleValues");
            return (List<T>) getPossibleValues.invoke(this);
        } catch(ReflectiveOperationException ignored) {
        }
        try {
            Method getValues = getClass().getMethod("getValues");
            return (List<T>) getValues.invoke(this);
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access property values.", e);
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private Class<T> typeCompat() {
        try {
            Method getValueClass = getClass().getMethod("getValueClass");
            return (Class<T>) getValueClass.invoke(this);
        } catch(ReflectiveOperationException ignored) {
        }
        try {
            Method getType = getClass().getMethod("getType");
            return (Class<T>) getType.invoke(this);
        } catch(ReflectiveOperationException ignored) {
        }
        try {
            Field type = getClass().getDeclaredField("type");
            type.setAccessible(true);
            return (Class<T>) type.get(this);
        } catch(ReflectiveOperationException ignored) {
        }
        throw new IllegalStateException("Unable to access property type.");
    }

    @Unique
    private String nameCompat() {
        try {
            Method getName = getClass().getMethod("getName");
            return (String) getName.invoke(this);
        } catch(ReflectiveOperationException ignored) {
        }
        try {
            Field name = getClass().getDeclaredField("name");
            name.setAccessible(true);
            return (String) name.get(this);
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access property name.", e);
        }
    }
}
