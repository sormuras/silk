/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Typed;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.lang.Type.raw;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * A {@link Hint} is a suggested reference for parameters of a
 * {@link Constructor} or {@link Method} to inject.
 *
 * @since 19.1
 *
 * @param <T> The {@link Type} of the argument
 */
public final class Hint<T> implements Typed<T> {

	private static final Hint<?>[] NO_PARAMS = new Hint<?>[0];

	public static Hint<?>[] none() {
		return NO_PARAMS;
	}

	public static <T> Hint<T> relativeReferenceTo(Class<T> target) {
		return relativeReferenceTo(raw(target));
	}

	/**
	 * A {@link Type} reference is relative to the injection site
	 * hierarchy. It can be pre-resolved for each site.
	 *
	 * @param target The type to resolve as parameter within the hierarchy
	 * @return A {@link Hint} representing the relative reference
	 */
	public static <T> Hint<T> relativeReferenceTo(Type<T> target) {
		return new Hint<>(target, null, anyOf(target), null);
	}

	/**
	 * An {@link Instance} reference is relative to the
	 * injection site hierarchy. It can be pre-resolved for each
	 * site.
	 *
	 * @param target The {@link Instance} to resolve as parameter within the
	 *            hierarchy
	 * @return A {@link Hint} representing the relative reference
	 */
	public static <T> Hint<T> relativeReferenceTo(Instance<T> target) {
		return new Hint<>(target.type, null, target, null);
	}

	/**
	 * A {@link Dependency} reference is absolute. That means it ignores the
	 * injection site hierarchy.
	 *
	 * @param target The {@link Dependency} to resolve as parameter
	 * @return A {@link Hint} representing the absolute reference
	 */
	public static <T> Hint<T> absoluteReferenceTo(Dependency<T> target) {
		return new Hint<>(target.type(), null, target.instance, target);
	}

	public static <T> Hint<T> constant(T constant) {
		if (constant == null)
			throw InconsistentDeclaration.incomprehensibleHint(null);
		@SuppressWarnings("unchecked")
		Type<T> type = (Type<T>) raw(constant.getClass());
		return new Hint<>(type, constant, null, null);
	}

	public static <T> Hint<T> constantNull(Type<T> asType) {
		return new Hint<>(asType, null, null, null);
	}

	public static Hint<?>[] match(Type<?>[] types, Hint<?>... hints) {
		if (types.length == 0)
			return NO_PARAMS;
		Hint<?>[] args = new Hint<?>[types.length];
		for (Hint<?> hint : hints) {
			int i = indexForType(types, hint, args);
			if (i < 0)
				throw InconsistentDeclaration.incomprehensibleHint(hint);
			args[i] = hint;
		}
		for (int i = 0; i < args.length; i++)
			if (args[i] == null)
				args[i] = Hint.relativeReferenceTo(types[i]);
		return args;
	}

	private static int indexForType(Type<?>[] types, Hint<?> hint,
			Hint<?>[] args) {
		for (int i = 0; i < types.length; i++)
			if (args[i] == null && hint.type().isAssignableTo(types[i]))
				return i;
		return -1;
	}

	public final T value;
	public final Instance<? extends T> relativeRef;
	public final Dependency<? extends T> absoluteRef;
	public final Type<T> asType;

	public Hint(Type<T> asType, T value, Instance<? extends T> relativeRef,
			Dependency<? extends T> absoluteRef) {
		this.asType = asType;
		this.value = value;
		this.relativeRef = relativeRef;
		this.absoluteRef = absoluteRef;
	}

	@Override
	public Type<T> type() {
		return asType;
	}

	public boolean isConstant() {
		return relativeRef == null && absoluteRef == null;
	}

	public <S> Hint<S> asType(Type<S> supertype) {
		return typed(supertype);
	}

	public <S> Hint<S> asType(Class<S> supertype) {
		return typed(raw(supertype));
	}

	/**
	 * @param type The new type of this {@link Hint}
	 * @throws ClassCastException In case the given type is incompatible with
	 *             the previous one.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <E> Hint<E> typed(Type<E> type) {
		asType.toSupertype(type);
		return new Hint<>(type, (E) value, (Instance<E>) relativeRef,
				(Dependency<E>) absoluteRef);
	}

	@Override
	public String toString() {
		if (isConstant())
			return "value as " + asType;
		return "ref to " + (absoluteRef != null ? absoluteRef : relativeRef)
			+ " as " + asType;
	}

	public Hint<?> withActualType(java.lang.reflect.Parameter param,
			Map<String, Type<?>> actualTypeArguments) {
		if (value != null || absoluteRef != null)
			return this;
		java.lang.reflect.Type genericType = param.getParameterizedType();
		Type<?> actualType = Type.type(genericType, actualTypeArguments);
		if (param.getType() == Type.class) {
			return constant(actualType.parameter(0));
		}
		return relativeReferenceTo(instance(relativeRef.name, actualType));
	}

}
