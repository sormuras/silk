/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.defaults;

import static se.jbee.inject.Cast.functionTypeOf;
import static se.jbee.inject.Cast.resourcesTypeFor;
import static se.jbee.inject.Scope.application;
import static se.jbee.inject.lang.Type.raw;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

import se.jbee.inject.*;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Toggled;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Supply;
import se.jbee.inject.config.Extension;
import se.jbee.inject.config.Plugins;
import se.jbee.inject.container.Lazy;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

/**
 * Installs all the build-in functionality by using the core API.
 */
public enum CoreFeature implements Toggled<CoreFeature> {
	/**
	 * Adds: {@link Provider}s can be injected for all bound types.
	 */
	PROVIDER(false),
	/**
	 * Adds: {@link List}s can be injected for all bound types (via array
	 * bridge)
	 */
	LIST(false),
	/**
	 * Adds: {@link Set} can be injected for all bound types (via array bridge)
	 */
	SET(false),
	/**
	 * Adds: {@link Collection} can be injected instead of {@link List} (needs
	 * explicit List bind).
	 */
	COLLECTION(false),
	/**
	 * Adds: {@link Logger}s can be injected per receiving class.
	 */
	LOGGER(false),
	/**
	 * Adds: Support for injection of {@link Optional}s. If {@link Optional}
	 * parameters cannot be resolved {@link Optional#empty()} is injected.
	 */
	OPTIONAL(false),
	/**
	 * Adds: That primitive arrays can be resolved for their wrapper types.
	 *
	 * Note that this only supports one-dimensional arrays of int, long, float,
	 * double and boolean. For further support add similar suppliers following
	 * the example given.
	 */
	PRIMITIVE_ARRAYS(false),
	/**
	 * Adds: Support for {@link Injector} sub-contexts.
	 */
	SUB_CONTEXT(true),
	/**
	 * Adds: Binds the bootstrapping {@link Env} as the {@link Name#DEFAULT}
	 * {@link Env} in the {@link Injector} context.
	 */
	ENV(true),
	/**
	 * Adds: The {@link DefaultScopes}
	 */
	SCOPES(true),
	/**
	 * Adds: {@link Extension}s via {@link ExtensionModule}.
	 */
	EXTENSION(true),
	/**
	 * Adds: {@link AnnotatedWith} via {@link AnnotatedWithModule}.
	 */
	ANNOTATED_WITH(true),
	/**
	 * Adds: {@link Obtainable}s
	 */
	OBTAINABLE(true)

	;

	public final boolean installedByDefault;

	CoreFeature(boolean installedByDefault) {
		this.installedByDefault = installedByDefault;
	}

	@Override
	public void bootstrap(
			Bootstrapper.ToggledBootstrapper<CoreFeature> bootstrapper) {
		bootstrapper.install(ListBridgeModule.class, LIST);
		bootstrapper.install(SetBridgeModule.class, SET);
		bootstrapper.install(CollectionBridgeModule.class, COLLECTION);
		bootstrapper.install(ProviderBridgeModule.class, PROVIDER);
		bootstrapper.install(LoggerModule.class, LOGGER);
		bootstrapper.install(OptionalBridgeModule.class, OPTIONAL);
		bootstrapper.install(SubContextModule.class, SUB_CONTEXT);
		bootstrapper.install(DefaultEnvModule.class, ENV);
		bootstrapper.install(DefaultScopes.class, SCOPES);
		bootstrapper.install(ExtensionModule.class, EXTENSION);
		bootstrapper.install(PrimitiveArraysModule.class, PRIMITIVE_ARRAYS);
		bootstrapper.install(AnnotatedWithModule.class, ANNOTATED_WITH);
		bootstrapper.install(ObtainableModule.class, OBTAINABLE);
	}

	private static class LoggerModule extends BinderModule {

		private static final Supplier<Logger> LOGGER = (dep, context) //
				-> Logger.getLogger(dep.target(1).type().rawType.getCanonicalName());

		@Override
		protected void declare() {
			per(Scope.targetInstance)//
					.starbind(Logger.class) //
					.toSupplier(LOGGER);
		}

	}

	private static class ProviderBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.dependency)//
					.starbind(Provider.class) //
					.toSupplier(Supply.PROVIDER);
		}

	}

	private static class ListBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.dependency)//
					.starbind(List.class) //
					.toSupplier(Supply.LIST_BRIDGE);
		}

	}

	private static class SetBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.dependency)//
					.starbind(Set.class) //
					.toSupplier(Supply.SET_BRIDGE);
		}

	}

	private static class CollectionBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault() //
					.per(Scope.dependency) //
					.starbind(Collection.class) //
					.toParametrized(List.class);
		}
	}

	private static class OptionalBridgeModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault() //
					.per(Scope.dependency) //
					.starbind(Optional.class) //
					.toSupplier((dep, context) -> optional(dep, context));
		}

		@SuppressWarnings("unchecked")
		<T> Optional<T> optional(Dependency<? super Optional<T>> dep, Injector context) {
			try {
				return Optional.ofNullable(
						(T) context.resolve(dep.onTypeParameter().uninject()));
			} catch (UnresolvableDependency e) {
				return Optional.empty();
			}
		}
	}

	private static final class SubContextModule extends BinderModule
			implements se.jbee.inject.Supplier<Injector>, Injector {

		@SuppressWarnings("rawtypes")
		public static final Type<Function<Class[], Injector>> INJECTOR_PROVIDER_TYPE = functionTypeOf(
				Class[].class, Injector.class);

		@Override
		protected void declare() {
			require(INJECTOR_PROVIDER_TYPE);
			asDefault() //
					.per(Scope.dependencyInstance) //
					.starbind(Injector.class) //
					.toSupplier(this);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Injector supply(Dependency<? super Injector> dep,
				Injector context) throws UnresolvableDependency {
			@SuppressWarnings("unchecked")
			Class<? extends Bundle>[] bundles = (Class<? extends Bundle>[]) //
			context.resolve(Plugins.class).forPoint(Injector.class,
					dep.instance.name.toString());
			if (bundles.length == 0)
				return this; // this module acts as an Injector that directly fails to resolve any Dependency
			return context.resolve(INJECTOR_PROVIDER_TYPE).apply(bundles);
		}

		@Override
		public <T> T resolve(Dependency<T> dep) throws UnresolvableDependency {
			throw new UnresolvableDependency.NoResourceForDependency(
					"Empty SubContext Injector", dep);
		}

	}

	private static final class ObtainableModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault() //
					.per(Scope.dependency) //
					.starbind(Obtainable.class) //
					.toSupplier((dep, context) -> obtain(dep, context));
		}

		@SuppressWarnings("unchecked")
		<T, E> Obtainable<T> obtain(Dependency<? super Obtainable<T>> dep, Injector context) {
			Dependency<T> targetDep = (Dependency<T>) dep.onTypeParameter().uninject();
			Type<T> targetType = targetDep.type();
			if (targetType.arrayDimensions() == 1) {
				Dependency<E> elementDep = (Dependency<E>) dep.typed(targetType.baseType());
				return new ObtainableCollection<>(context, elementDep);
			}
			return new ObtainableInstance<>(() -> context.resolve(targetDep));
		}

		static final class ObtainableCollection<T, E> implements Obtainable<T> {

			private final Injector context;
			private final Dependency<E> dep;
			private final Lazy<T> value = new Lazy<>();

			ObtainableCollection(Injector context, Dependency<E> dep) {
				this.context = context;
				this.dep = dep;
			}

			@SuppressWarnings("unchecked")
			private T resolve() {
				Resource<E>[] resources = context.resolve(
						dep.typed(resourcesTypeFor(dep.type())));
				List<E> elements = new ArrayList<>();
				for (Resource<E> r : resources) {
					try {
						elements.add(r.generate(dep));
					} catch (UnresolvableDependency e) {
						// ignored
					}
				}
				return (T) Utils.arrayOf(elements, dep.type().rawType);
			}

			@Override
			public T obtain() {
				return value.get(this::resolve);
			}
		}

		static final class ObtainableInstance<T> implements Obtainable<T> {

			private final Provider<T> resolver;
			private final Lazy<T> value = new Lazy<>();
			private UnresolvableDependency caught;

			ObtainableInstance(Provider<T> resolver) {
				this.resolver = resolver;
			}

			private T resolve() {
				try {
					return resolver.provide();
				} catch (UnresolvableDependency e) {
					caught = e;
					return null;
				}
			}

			@Override
			public T obtain() {
				return value.get(this::resolve);
			}

			@Override
			public <X extends Exception> T orElseThrow(
					Function<UnresolvableDependency, ? extends X> exceptionTransformer)
					throws X {
				T res = obtain();
				if (res != null)
					return res;
				throw exceptionTransformer.apply(caught);
			}
		}
	}

	private static final class DefaultEnvModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(Env.class).to(env());
		}

	}

	private static final class PrimitiveArraysModule extends BinderModule {

		@Override
		protected void declare() {
			ScopedBinder asDefault = asDefault().per(application);
			asDefault.bind(int[].class) //
					.toSupplier(PrimitiveArraysModule::ints);
			asDefault.bind(long[].class) //
					.toSupplier(PrimitiveArraysModule::longs);
			asDefault.bind(float[].class) //
					.toSupplier(PrimitiveArraysModule::floats);
			asDefault.bind(double[].class) //
					.toSupplier(PrimitiveArraysModule::doubles);
			asDefault.bind(boolean[].class) //
					.toSupplier(PrimitiveArraysModule::booleans);
		}

		private static <T> T copyToPrimitiveArray(Object[] src, T dest) {
			for (int i = 0; i < src.length; i++)
				Array.set(dest, i, src[i]);
			return dest;
		}

		private static int[] ints(Dependency<? super int[]> dep,
				Injector context) {
			Integer[] wrappers = context.resolve(
					dep.typed(raw(Integer[].class)));
			return copyToPrimitiveArray(wrappers, new int[wrappers.length]);
		}

		public static long[] longs(Dependency<? super long[]> dep,
				Injector context) {
			Long[] wrappers = context.resolve(dep.typed(raw(Long[].class)));
			return copyToPrimitiveArray(wrappers, new long[wrappers.length]);
		}

		public static float[] floats(Dependency<? super float[]> dep,
				Injector context) {
			Float[] wrappers = context.resolve(dep.typed(raw(Float[].class)));
			return copyToPrimitiveArray(wrappers, new float[wrappers.length]);
		}

		public static double[] doubles(Dependency<? super double[]> dep,
				Injector context) {
			Double[] wrappers = context.resolve(dep.typed(raw(Double[].class)));
			return copyToPrimitiveArray(wrappers, new double[wrappers.length]);
		}

		public static boolean[] booleans(Dependency<? super boolean[]> dep,
				Injector context) {
			Boolean[] wrappers = context.resolve(
					dep.typed(raw(Boolean[].class)));
			return copyToPrimitiveArray(wrappers, new boolean[wrappers.length]);
		}
	}

}
