/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Cast.resourceTypeFor;

/**
 * Similar to a call-site each {@linkplain InjectionSite} represents the
 * resolution of arguments from a specific site or path that is represented by a
 * {@link Dependency}. This is used in cases where otherwise a dependency would
 * be resolved over and over again for the same {@link Dependency}. For example
 * when injecting a factory {@link java.lang.reflect.Method} or
 * {@link java.lang.reflect.Constructor} with arguments.
 */
public final class InjectionSite {

	public final Dependency<?> site;

	private final Hint<?>[] hints;
	private final Generator<?>[] generators;
	private final Object[] preResolvedArgs;
	private final int[] lazyArgIndexes;
	private final int lazyArgCount;

	public InjectionSite(Injector injector, Dependency<?> site,
			Hint<?>[] hints) {
		this.site = site;
		this.hints = hints;
		this.generators = new Generator<?>[hints.length];
		this.preResolvedArgs = new Object[hints.length];
		this.lazyArgIndexes = new int[hints.length];
		this.lazyArgCount = preResolveArgs(injector);
	}

	public Object[] args(Injector injector) throws UnresolvableDependency {
		if (lazyArgCount == 0)
			return preResolvedArgs;
		// in this case we have to copy to become thread-safe!
		Object[] args = preResolvedArgs.clone();
		for (int j = 0; j < lazyArgCount; j++) {
			int i = lazyArgIndexes[j];
			Hint<?> hint = hints[i];
			args[i] = generators[i] == null
				? injector.resolve(site.instanced(hint.relativeRef))
				: generate(generators[i], site.instanced(hint.relativeRef));
		}
		return args;
	}

	private int preResolveArgs(Injector injector) {
		int lazyArgIndex = 0;
		for (int i = 0; i < generators.length; i++) {
			Hint<?> hint = hints[i];
			if (hint.type().rawType == Injector.class) {
				preResolvedArgs[i] = injector;
			} else if (hint.isConstant()) {
				preResolvedArgs[i] = hint.value;
			} else if (hint.type().arrayDimensions() == 1) {
				lazyArgIndexes[lazyArgIndex++] = i;
			} else if (hint.absoluteRef != null) {
				preResolvedArgs[i] = injector.resolve(hint.absoluteRef);
			} else { // relative ref
				Instance<?> ref = hint.relativeRef;
				Dependency<? extends Resource<?>> resourceDep = site.typed(
						resourceTypeFor(ref.type)).named(ref.name);
				Resource<?> resource = injector.resolve(resourceDep);
				if (resource.permanence.isPermanent()) {
					//TODO and not has type variable involved
					preResolvedArgs[i] = generate(resource,
							site.instanced(hint.relativeRef));
				} else {
					lazyArgIndexes[lazyArgIndex++] = i;
					generators[i] = resource;
				}
			}
		}
		return lazyArgIndex;
	}

	@SuppressWarnings("unchecked")
	private static <I> I generate(Generator<I> gen, Dependency<?> dep) {
		return gen.generate((Dependency<? super I>) dep);
	}
}
