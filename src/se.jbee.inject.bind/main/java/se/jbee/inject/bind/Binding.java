/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import static se.jbee.inject.lang.Utils.arrayOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.*;
import se.jbee.inject.lang.Qualifying;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Typed;

/**
 * A {@link Binding} is implements the {@link ResourceDescriptor} created during
 * the bootstrapping process based on {@link Bindings}, {@link Bundle}s and
 * {@link Module}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 *
 * @param <T> The type of the bound value (instance)
 */
public final class Binding<T> extends ResourceDescriptor<T>
		implements Module, Typed<T>, Comparable<Binding<?>> {

	public static <T> Binding<T> binding(Locator<T> signature, BindingType type,
			Supplier<? extends T> supplier, Name scope, Source source) {
		return new Binding<>(signature, type, supplier, scope, source,
				annotatedOf(supplier), Verifier.AOK);
	}

	public final BindingType type;

	private Binding(Locator<T> signature, BindingType type,
			Supplier<? extends T> supplier, Name scope, Source source,
			Annotated annotations, Verifier verifier) {
		super(scope, signature, supplier, source, annotations, verifier);
		this.type = type;
	}

	@Override
	public Type<T> type() {
		return signature.type();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Binding<E> typed(Type<E> type) {
		return new Binding<>(signature.typed(type().toSupertype(type)),
				this.type, (Supplier<? extends E>) supplier, scope, source,
				annotations, verifier);
	}

	public boolean isComplete() {
		return supplier != null;
	}

	@Override
	public Binding<T> annotatedBy(Annotated annotations) {
		if (annotations == this.annotations)
			return this; // just a optimisation for a likely case
		return new Binding<>(signature, type, supplier, scope, source,
				annotations, verifier);
	}

	@Override
	public Binding<T> verifiedBy(Verifier verifier) {
		if (verifier == this.verifier)
			return this;
		return new Binding<>(signature, type, supplier, scope, source,
				annotations, verifier);
	}

	public Binding<T> complete(BindingType type,
			Supplier<? extends T> supplier) {
		if (type == BindingType.VALUE)
			throw InconsistentBinding.illegalCompletion(this, type);
		Name effectiveScope = type == BindingType.REFERENCE
			? Scope.reference
			: scope;
		return new Binding<>(signature, type, supplier, effectiveScope, source,
				annotatedOf(supplier), verifier);
	}

	@Override
	public void declare(Bindings bindings, Env env) {
		bindings.add(env, this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Binding))
			return false;
		Binding<?> other = (Binding<?>) obj;
		return signature.equalTo(other.signature)
			&& source.equalTo(other.source) && scope.equalTo(other.scope)
			&& type == other.type;
	}

	@Override
	public int hashCode() {
		return signature.hashCode() ^ source.hashCode();
	}

	@Override
	public int compareTo(Binding<?> other) {
		int res = signature.type().rawType.getName().compareTo(
				other.signature.type().rawType.getName());
		if (res != 0)
			return res;
		res = Qualifying.compare(signature.instance, other.signature.instance);
		if (res != 0)
			return res;
		res = signature.instance.compareTo(other.signature.instance);
		if (res != 0)
			return res;
		res = Qualifying.compare(signature.target, other.signature.target);
		if (res != 0)
			return res;
		res = signature.target.compareTo(other.signature.target);
		if (res != 0)
			return res;
		res = Qualifying.compare(source, other.source);
		if (res != 0)
			return res;
		res = source.compareTo(other.source);
		if (res != 0)
			return res;
		res = scope.compareTo(other.scope);
		if (res != 0)
			return res;
		return type.compareTo(other.type);
	}

	/**
	 * Removes those bindings that are ambiguous but also do not clash because
	 * of different {@link DeclarationType}s that replace each other.
	 */
	public static Binding<?>[] disambiguate(Binding<?>[] bindings) {
		if (bindings.length <= 1)
			return bindings;
		List<Binding<?>> uniques = new ArrayList<>(bindings.length);
		Arrays.sort(bindings);
		uniques.add(bindings[0]);
		int lastUniqueIndex = 0;
		Set<Type<?>> required = new HashSet<>();
		List<Binding<?>> dropped = new ArrayList<>();
		for (int i = 1; i < bindings.length; i++) {
			Binding<?> lastUnique = bindings[lastUniqueIndex];
			Binding<?> current = bindings[i];
			final boolean equalResource = lastUnique.signature.equalTo(
					current.signature);
			DeclarationType lastType = lastUnique.source.declarationType;
			DeclarationType curType = current.source.declarationType;
			if (equalResource && lastType.clashesWith(curType))
				throw InconsistentBinding.clash(lastUnique, current);
			if (curType == DeclarationType.REQUIRED) {
				required.add(current.signature.type());
			} else if (equalResource && (lastType.droppedWith(curType))) {
				if (!isDuplicateIdenticalConstant(true, lastUnique,
						current) && i - 1 == lastUniqueIndex) {
						dropped.add(uniques.remove(uniques.size() - 1));
				}
				dropped.add(current);
			} else if (!equalResource || !curType.replacedBy(lastType)) {
				if (current.source.declarationType == DeclarationType.MULTI
					&& isDuplicateIdenticalConstant(equalResource, lastUnique,
							current)) {
					dropped.add(current);
				} else {
					uniques.add(current);
					lastUniqueIndex = i;
				}
			}
		}
		return withoutProvidedThatAreNotRequiredIn(uniques, required, dropped);
	}

	private static boolean isDuplicateIdenticalConstant(boolean equalResource,
			Binding<?> lastUnique, Binding<?> current) {
		return equalResource && current.type == BindingType.PREDEFINED
			&& lastUnique.supplier.equals(current.supplier);
	}

	private static Binding<?>[] withoutProvidedThatAreNotRequiredIn(
			List<Binding<?>> bindings, Set<Type<?>> required,
			List<Binding<?>> dropped) {
		List<Binding<?>> res = new ArrayList<>(bindings.size());
		for (Binding<?> b : bindings) {
			Type<?> type = b.signature.type();
			if (b.source.declarationType != DeclarationType.PROVIDED
				|| required.contains(type)) {
				res.add(b);
				required.remove(type);
			}
		}
		if (!required.isEmpty())
			throw new UnresolvableDependency.NoResourceForDependency(required,
					dropped);
		return arrayOf(res, Binding.class);
	}

}
