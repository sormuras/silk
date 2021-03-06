/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.*;
import se.jbee.inject.bind.*;
import se.jbee.inject.config.*;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static se.jbee.inject.Cast.initialiserTypeOf;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Hint.relativeReferenceTo;
import static se.jbee.inject.Instance.*;
import static se.jbee.inject.Source.source;
import static se.jbee.inject.Target.targeting;
import static se.jbee.inject.lang.Type.fieldType;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Utils.isClassInstantiable;
import static se.jbee.inject.lang.Utils.newArray;
import static se.jbee.inject.config.Plugins.pluginPoint;

/**
 * The default implementation of a fluent binder interface that provides a lot
 * of utility methods to improve readability and keep binding compact.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings({ "squid:S1448", "squid:S1200", "ClassReferencesSubclass" })
public class Binder {

	public static RootBinder create(Bind bind) {
		return new RootBinder(bind);
	}

	protected final RootBinder root;
	private final Bind bind;

	Binder(RootBinder root, Bind bind) {
		this.root = root == null ? (RootBinder) this : root;
		this.bind = bind.source == null ? bind.with(source(getClass())) : bind;
	}

	public Bind bind() {
		return bind; // !ATTENTION! This method might be overridden to update
					// Bind properties - do not access field directly
	}

	protected final Env env() {
		return bind().env;
	}

	protected final Bindings bindings() {
		return bind().bindings;
	}

	//TODO make this into a Module subclass
	public void addAnnotated(Class<?>... types) {
		Bindings bindings = bindings();
		for (Class<?> type : types)
			bindings.addAnnotated(bind().env, type);
	}

	/**
	 * Allows access only via interface.
	 *
	 * @since 19.1
	 *
	 * @return fluent API
	 */
	public Binder withIndirectAccess() {
		return with(bind().target.indirect());
	}

	public <E> TypedElementBinder<E> arraybind(Class<E[]> type) {
		return new TypedElementBinder<>(bind(), defaultInstanceOf(raw(type)));
	}

	public <T> TypedBinder<T> autobind(Class<T> type) {
		return autobind(Type.raw(type));
	}

	public <T> TypedBinder<T> autobind(Type<T> type) {
		return on(bind().asAuto()).bind(type);
	}

	public <T> TypedBinder<T> bind(Class<T> type) {
		return bind(Type.raw(type));
	}

	public <T> TypedBinder<T> bind(Instance<T> instance) {
		return new TypedBinder<>(bind(), instance);
	}

	public <T> TypedBinder<T> bind(Name name, Class<T> type) {
		return bind(name, Type.raw(type));
	}

	public <T> TypedBinder<T> bind(Name name, Type<T> type) {
		return bind(instance(name, type));
	}

	public <T> TypedBinder<T> bind(Type<T> type) {
		return bind(defaultInstanceOf(type));
	}

	public void construct(Class<?> type) {
		construct((defaultInstanceOf(raw(type))));
	}

	public void construct(Instance<?> instance) {
		bind(instance).toConstructor();
	}

	public void construct(Name name, Class<?> type) {
		construct(instance(name, raw(type)));
	}

	/**
	 * Bind something that is an {@link Initialiser} for the {@link Injector}.
	 *
	 * @since 19.1
	 *
	 * @return fluent API
	 */
	public TypedBinder<Initialiser<Injector>> initbind() {
		return initbind(Injector.class);
	}

	/**
	 * @since 19.1
	 *
	 * @return fluent API
	 */
	public <T> TypedBinder<Initialiser<T>> initbind(Class<T> type) {
		return initbind(raw(type));
	}

	/**
	 * @since 19.1
	 *
	 * @return fluent API
	 */
	public <T> TypedBinder<Initialiser<T>> initbind(Type<T> type) {
		return multibind(initialiserTypeOf(type));
	}

	public <T> TypedBinder<T> multibind(Class<T> type) {
		return multibind(raw(type));
	}

	public <T> TypedBinder<T> multibind(Instance<T> instance) {
		return on(bind().asMulti()).bind(instance);
	}

	public <T> TypedBinder<T> multibind(Name name, Class<T> type) {
		return multibind(instance(name, raw(type)));
	}

	public <T> TypedBinder<T> multibind(Name name, Type<T> type) {
		return multibind(instance(name, type));
	}

	public <T> TypedBinder<T> multibind(Type<T> type) {
		return multibind(defaultInstanceOf(type));
	}

	public <T> TypedBinder<T> starbind(Class<T> type) {
		return bind(anyOf(raw(type)));
	}

	public <T> PluginBinder<T> plug(Class<T> plugin) {
		return new PluginBinder<>(on(bind()), plugin);
	}

	protected Binder on(Bind bind) {
		return new Binder(root, bind);
	}

	protected final Binder implicit() {
		return on(bind().asImplicit());
	}

	protected final Binder with(Target target) {
		return new Binder(root, bind().with(target));
	}

	/**
	 * @see #installIn(String, Class...)
	 * @since 19.1
	 */
	@SafeVarargs
	public final void installIn(Class<?> subContext,
			Class<? extends Bundle>... lazyInstalled) {
		installIn(subContext.getName(), lazyInstalled);
	}

	/**
	 * Binds a lazy {@link Bundle} which is installed in a sub-context
	 * {@link Injector}. The {@link Injector} is bootstrapped lazily on first
	 * usage. Use {@link Injector#subContext(String)} to resolve the sub-context
	 * by name.
	 *
	 * @param subContext the name of the lazy {@link Injector} sub-context
	 * @param lazyInstalled the {@link Bundle} to install in the sub-context
	 *            lazy {@link Injector}
	 *
	 * @since 19.1
	 */
	@SafeVarargs
	public final void installIn(String subContext,
			Class<? extends Bundle>... lazyInstalled) {
		if (lazyInstalled.length > 0) {
			for (Class<? extends Bundle> bundle : lazyInstalled)
				plug(bundle).into(Injector.class, subContext);
		}
	}

	/**
	 *
	 * @param target the type whose instances should be initialised by calling
	 *            some method
	 * @since 19.1
	 */
	public <T> InitBinder<T> init(Class<T> target) {
		return init(Name.DEFAULT, raw(target));
	}

	public <T> InitBinder<T> init(Name name, Type<T> target) {
		return new InitBinder<>(on(bind()), instance(name, target));
	}

	/**
	 * Small utility to make initialisation calls that depend on instances
	 * managed by the {@link Injector} easier.
	 *
	 * The basic principle is that the {@link #target} {@link Instance} is
	 * initialised on the basis of some other dependency instance that is
	 * resolved during initialisation phase and provided to the
	 * {@link BiConsumer} function.
	 *
	 * @param <T> type of the instances that should be initialised
	 *
	 * @since 19.1
	 */
	public static class InitBinder<T> {

		private final Binder binder;
		private final Instance<T> target;

		protected InitBinder(Binder binder, Instance<T> target) {
			this.binder = binder;
			this.target = target;
		}

		public <C> void forAny(Class<? extends C> dependency,
				BiConsumer<T, C> initialiser) {
			forEach(raw(dependency).addArrayDimension().asUpperBound(),
					initialiser);
		}

		public <C> void forEach(Type<? extends C[]> dependencies,
				BiConsumer<T, C> initialiser) {
			binder.initbind().to((impl, injector) -> {
				T obj = injector.resolve(target);
				C[] args = injector.resolve(
						dependency(dependencies).injectingInto(target));
				for (C arg : args)
					initialiser.accept(obj, arg);
				return impl;
			});
		}

		public <C> void by(Class<? extends C> dependency,
				BiConsumer<T, C> initialiser) {
			by(defaultInstanceOf(raw(dependency)), initialiser);
		}

		public <C> void by(Name depName, Class<? extends C> depType,
				BiConsumer<T, C> initialiser) {
			by(Instance.instance(depName, raw(depType)), initialiser);
		}

		public <C> void by(Instance<? extends C> dependency,
				BiConsumer<T, C> initialiser) {
			binder.initbind().to((impl, injector) -> {
				T obj = injector.resolve(target);
				C arg = injector.resolve(
						dependency(dependency).injectingInto(target));
				initialiser.accept(obj, arg);
				return impl;
			});
		}
	}

	public static class PluginBinder<T> {

		private final Binder binder;
		private final Class<T> plugin;

		protected PluginBinder(Binder binder, Class<T> plugin) {
			this.binder = binder;
			this.plugin = plugin;
		}

		public void into(Class<?> pluginPoint) {
			into(pluginPoint, plugin.getCanonicalName());
		}

		public void into(Class<?> pluginPoint, String property) {
			binder.multibind(pluginPoint(pluginPoint, property),
					Class.class).to(plugin);
			if (isClassInstantiable(plugin))
				binder.implicit().construct(plugin);
			// we allow both collections of classes that have a common
			// super-type or collections that don't
			if (raw(plugin).isAssignableTo(raw(pluginPoint).asUpperBound())
				&& !plugin.isAnnotation()) {
				// if they have a common super-type the plugin is bound as an
				// implementation
				@SuppressWarnings("unchecked")
				Class<? super T> pp = (Class<? super T>) pluginPoint;
				binder.multibind(pp).to(plugin);
			}
		}
	}

	/**
	 * The {@link AutoBinder} makes use of mirrors to select and bind
	 * constructors for beans and methods as factories and {@link Name} these
	 * instances as well as provide {@link Hint}s.
	 *
	 * @since 19.1
	 */
	public static class AutoBinder {

		private final RootBinder binder;
		private final SharesBy sharesBy;
		private final ConstructsBy constructsBy;
		private final ProducesBy producesBy;
		private final NamesBy namesBy;
		private final ScopesBy scopesBy; //TODO use it
		private final HintsBy hintsBy;

		protected AutoBinder(RootBinder binder, Name scope) {
			Bind bind = binder.bind();
			this.binder = binder.on(bind.asAuto()).on(bind.next());
			Env env = bind.env;
			Package where = bind.source.pkg();
			this.sharesBy = env.property(SharesBy.class, where);
			this.constructsBy = env.property(ConstructsBy.class, where);
			this.producesBy = env.property(ProducesBy.class, where);
			this.namesBy = env.property(NamesBy.class, where);
			this.hintsBy = env.property(HintsBy.class, where);
			this.scopesBy = scope.equalTo(Scope.mirror)
				? env.property(ScopesBy.class, where)
				: target -> scope;
		}

		private AutoBinder(RootBinder binder, SharesBy constantsBy,
				ConstructsBy constructsBy, ProducesBy producesBy,
				NamesBy namesBy, ScopesBy scopesBy, HintsBy hintsBy) {
			this.binder = binder;
			this.sharesBy = constantsBy;
			this.constructsBy = constructsBy;
			this.producesBy = producesBy;
			this.namesBy = namesBy;
			this.scopesBy = scopesBy;
			this.hintsBy = hintsBy;
		}

		public AutoBinder shareBy(SharesBy mirror) {
			return new AutoBinder(binder, mirror, constructsBy, producesBy,
					namesBy, scopesBy, hintsBy);
		}

		public AutoBinder constructBy(ConstructsBy mirror) {
			return new AutoBinder(binder, sharesBy, mirror, producesBy, namesBy,
					scopesBy, hintsBy);
		}

		public AutoBinder produceBy(ProducesBy mirror) {
			return new AutoBinder(binder, sharesBy, constructsBy, mirror,
					namesBy, scopesBy, hintsBy);
		}

		public AutoBinder nameBy(NamesBy mirror) {
			return new AutoBinder(binder, sharesBy, constructsBy, producesBy,
					mirror, scopesBy, hintsBy);
		}

		public AutoBinder scopeBy(ScopesBy mirror) {
			return new AutoBinder(binder, sharesBy, constructsBy, producesBy,
					namesBy, mirror, hintsBy);
		}

		public AutoBinder hintBy(HintsBy mirror) {
			return new AutoBinder(binder, sharesBy, constructsBy, producesBy,
					namesBy, scopesBy, mirror);
		}

		public void in(Class<?> service) {
			in(service, Hint.none());
		}

		public void in(Object service, Hint<?>... hints) {
			in(service.getClass(), service, hints);
		}

		public void in(Class<?> service, Hint<?>... hints) {
			in(service, null, hints);
		}

		private void in(Class<?> service, Object instance,
				Hint<?>... hints) {
			boolean needsInstance1 = bindProducesIn(service, instance, hints);
			boolean needsInstance2 = bindSharesIn(service, instance);
			if (!needsInstance1 && !needsInstance2)
				return; // do not try to construct the class
			Constructor<?> target = constructsBy.reflect(service);
			if (target != null)
				bind(target, hints);
		}

		private boolean bindSharesIn(Class<?> impl, Object instance) {
			boolean needsInstance = false;
			for (Field constant : sharesBy.reflect(impl)) {
				binder.per(Scope.container).bind(namesBy.reflect(constant),
						fieldType(constant)).to(instance, constant);
				needsInstance |= !isStatic(constant.getModifiers());
			}
			return needsInstance;
		}

		private boolean bindProducesIn(Class<?> impl, Object instance,
				Hint<?>[] hints) {
			boolean needsInstance = false;
			for (Method producer : producesBy.reflect(impl)) {
				Type<?> returns = Type.returnType(producer);
				if (returns.rawType != void.class
					&& returns.rawType != Void.class) {
					if (hints.length == 0)
						hints = hintsBy.reflect(producer);
					binder.per(scopesBy.reflect(producer)) //
							.bind(namesBy.reflect(producer), returns) //
							.to(instance, producer, hints);
					needsInstance |= !isStatic(producer.getModifiers());
				}
			}
			return needsInstance;
		}

		private <T> void bind(Constructor<T> target, Hint<?>... hints) {
			Name name = namesBy.reflect(target);
			if (hints.length == 0)
				hints = hintsBy.reflect(target);
			Class<T> impl = target.getDeclaringClass();
			Binder appBinder = binder.per(Scope.application).implicit();
			if (name.isDefault()) {
				appBinder.autobind(impl).to(target, hints);
			} else {
				appBinder.bind(name, impl).to(target, hints);
				for (Type<? super T> st : Type.raw(impl).supertypes())
					if (st.isInterface())
						appBinder.implicit().bind(name, st).to(name, impl);
			}
		}

		public void in(Class<?> impl, Class<?>... more) {
			in(impl);
			for (Class<?> i : more)
				in(i);
		}
	}

	public static class RootBinder extends ScopedBinder {

		public RootBinder(Bind bind) {
			super(null, bind);
		}

		public ScopedBinder per(Name scope) {
			return new ScopedBinder(root, bind().per(scope));
		}

		public RootBinder asDefault() {
			return on(bind().asDefault());
		}

		// OPEN also allow naming for provided instances - this is used for
		// value objects that become parameter; settings required and provided

		public <T> void provide(Class<T> impl, Hint<?>... hints) {
			on(bind().asProvided()).bind(impl).toConstructor(hints);
		}

		public <T> void require(Class<T> dependency) {
			require(raw(dependency));
		}

		public <T> void require(Type<T> dependency) {
			on(bind().asRequired()).bind(dependency).to(Supply.required(),
					BindingType.REQUIRED);
		}

		@Override
		protected RootBinder on(Bind bind) {
			return new RootBinder(bind);
		}

	}

	public static class ScopedBinder extends TargetedBinder {

		protected ScopedBinder(RootBinder root, Bind bind) {
			super(root, bind);
		}

		/**
		 * Root for container "global" configuration.
		 *
		 * @since 19.1
		 */
		public TargetedBinder config() {
			return injectingInto(Config.class);
		}

		/**
		 * Root for target type specific configuration.
		 *
		 * @since 19.1
		 */
		public TargetedBinder config(Class<?> ns) {
			return config().within(ns);
		}

		/**
		 * Root for {@link Instance} specific configuration.
		 *
		 * @since 19.1
		 */
		public TargetedBinder config(Instance<?> ns) {
			return config().within(ns);
		}

		public TargetedBinder injectingInto(Class<?> target) {
			return injectingInto(raw(target));
		}

		public TargetedBinder injectingInto(Instance<?> target) {
			return new TargetedBinder(root, bind().with(targeting(target)));
		}

		public TargetedBinder injectingInto(Name name, Class<?> type) {
			return injectingInto(name, raw(type));
		}

		public TargetedBinder injectingInto(Name name, Type<?> type) {
			return injectingInto(Instance.instance(name, type));
		}

		public TargetedBinder injectingInto(Type<?> target) {
			return injectingInto(defaultInstanceOf(target));
		}

		/**
		 * Bind {@link Method}s and {@link Constructor}s based on mirrors.
		 *
		 * @since 19.1
		 */
		public AutoBinder autobind() {
			return new AutoBinder(root, bind().scope);
		}
	}

	public static class TargetedBinder extends Binder {

		protected TargetedBinder(RootBinder root, Bind bind) {
			super(root, bind);
		}

		public Binder in(Packages packages) {
			return with(bind().target.in(packages));
		}

		public Binder inPackageAndSubPackagesOf(Class<?> type) {
			return with(bind().target.inPackageAndSubPackagesOf(type));
		}

		public Binder inPackageOf(Class<?> type) {
			return with(bind().target.inPackageOf(type));
		}

		public Binder inSubPackagesOf(Class<?> type) {
			return with(bind().target.inSubPackagesOf(type));
		}

		public TargetedBinder within(Class<?> parent) {
			return within(raw(parent));
		}

		public TargetedBinder within(Instance<?> parent) {
			return new TargetedBinder(root, bind().within(parent));
		}

		public TargetedBinder within(Name name, Class<?> parent) {
			return within(instance(name, raw(parent)));
		}

		public TargetedBinder within(Name name, Type<?> parent) {
			return within(instance(name, parent));
		}

		public TargetedBinder within(Type<?> parent) {
			return within(anyOf(parent));
		}
	}

	public static class TypedBinder<T> {

		private final Bind bind;
		protected final Locator<T> locator;

		protected TypedBinder(Bind bind, Instance<T> instance) {
			this(bind.next(), new Locator<>(instance, bind.target));
		}

		TypedBinder(Bind bind, Locator<T> locator) {
			this.bind = bind;
			this.locator = locator;
		}

		private Env env() {
			return bind().env;
		}

		private <P> P env(Class<P> property) {
			return env().property(property, bind().source.pkg());
		}

		public <I extends T> void to(Class<I> impl) {
			to(Instance.anyOf(raw(impl)));
		}

		public void to(Constructor<? extends T> target, Hint<?>... hints) {
			if (hints.length == 0)
				hints = env(HintsBy.class).reflect(target);
			expand(New.newInstance(target, hints));
		}

		protected final void to(Object owner, Method target,
				Hint<?>... hints) {
			if (hints.length == 0)
				hints = env(HintsBy.class).reflect(target);
			expand(Produces.produces(owner, target, hints));
		}

		protected final void to(Object owner, Field constant) {
			expand(Shares.shares(owner, constant));
		}

		protected final void expand(Object value) {
			declareBindingsIn(bind().asType(locator, BindingType.VALUE, null),
					value);
		}

		protected final void expand(BindingType type,
				Supplier<? extends T> supplier) {
			Binding<T> binding = bind().asType(locator, type, supplier);
			declareBindingsIn(binding, binding);
		}

		private void declareBindingsIn(Binding<?> binding, Object value) {
			bindings().addExpanded(env(), binding, value);
		}

		public void toSupplier(Supplier<? extends T> supplier) {
			to(supplier, BindingType.PREDEFINED);
		}

		public <I extends Supplier<? extends T>> void toSupplier(Function<Injector, I> factory) {
			AtomicReference<I> cache = new AtomicReference<>();
			toSupplier((Supplier<? extends T>) (dep, context) ->
					cache.updateAndGet(e -> e != null ? e :
							factory.apply(context)).supply(dep, context));
		}

		/**
		 * Utility method that creates the instances from the {@link Injector}
		 * context given.
		 *
		 * This is used when a full {@link Supplier} contract is not needed to
		 * save stating the not needed {@link Dependency} argument.
		 *
		 * @since 19.1
		 */
		public void toFactory(Function<Injector, T> factory) {
			toSupplier((dep, context) -> factory.apply(context));
		}

		/**
		 * This method will bind the provided {@link Generator} in a way that
		 * bypasses {@link Scope} effects. The provided {@link Generator} is
		 * directly called to generate the instance each time it should be
		 * injected.
		 *
		 * If a {@link Scope} should apply use {@link #toSupplier(Supplier)}
		 * instead or create a {@link Generator} bridge that does not implement
		 * {@link Generator} itself.
		 *
		 * @since 19.1
		 */
		public void toGenerator(Generator<? extends T> generator) {
			toSupplier(new SupplierGeneratorBridge<>(generator));
		}

		/**
		 * @since 19.1
		 */
		public void to(java.util.function.Supplier<? extends T> method) {
			toSupplier((Dependency<? super T> d, Injector i) -> method.get());
		}

		/**
		 * By default constants are not scoped. This implies that no
		 * initialisation occurs for constants.
		 *
		 * In contrast to {@link #to(Object)} a scoped constant exist within a
		 * {@link Scope} like instances created by the container. This has the
		 * effect of running initialisation for the provided constant similar to
		 * the initialisation that occurs for instances created by the
		 * container.
		 *
		 * @since 19.1
		 *
		 * @param constant a "bean" instance
		 */
		public final void toScoped(T constant) {
			expand(new Constant<>(constant).scoped());
		}

		public final void to(T constant) {
			toConstant(constant);
		}

		public final void to(T constant1, T constant2) {
			onMulti().toConstant(constant1).toConstant(constant2);
		}

		public final void to(T constant1, T constant2, T constant3) {
			onMulti().toConstant(constant1).toConstant(constant2).toConstant(
					constant3);
		}

		@SafeVarargs
		public final void to(T constant1, T... constants) {
			TypedBinder<T> multibinder = onMulti().toConstant(constant1);
			for (T constant : constants)
				multibinder.toConstant(constant);
		}

		public void toConstructor() {
			to(env(ConstructsBy.class).reflect(locator.type().rawType));
		}

		public void toConstructor(Class<? extends T> impl,
				Hint<?>... hints) {
			if (!isClassInstantiable(impl))
				throw InconsistentDeclaration.notConstructable(impl);
			to(env(ConstructsBy.class).reflect(impl), hints);
		}

		public void toConstructor(Hint<?>... hints) {
			toConstructor(getType().rawType, hints);
		}

		public <I extends T> void to(Name name, Class<I> type) {
			to(instance(name, raw(type)));
		}

		public <I extends T> void to(Name name, Type<I> type) {
			to(instance(name, type));
		}

		public <I extends T> void to(Instance<I> instance) {
			expand(instance);
		}

		public <I extends T> void toParametrized(Class<I> impl) {
			expand(impl);
		}

		public <I extends Supplier<? extends T>> void toSupplier(
				Class<I> impl) {
			expand(defaultInstanceOf(raw(impl)));
		}

		protected final void to(Supplier<? extends T> supplier,
				BindingType type) {
			expand(type, supplier);
		}

		private TypedBinder<T> toConstant(T constant) {
			expand(new Constant<>(constant));
			return this;
		}

		final Bind bind() {
			return bind;
		}

		protected final Bindings bindings() {
			return bind().bindings;
		}

		protected final Type<T> getType() {
			return locator.type();
		}

		protected final TypedBinder<T> on(Bind bind) {
			return new TypedBinder<>(bind, locator);
		}

		protected final TypedBinder<T> onMulti() {
			return on(bind().asMulti());
		}

	}

	/**
	 * This kind of bindings actually re-map the []-type so that the automatic
	 * behavior of returning all known instances of the element type will no
	 * longer be used whenever the bind made applies.
	 *
	 * @author Jan Bernitt (jan@jbee.se)
	 *
	 */
	public static class TypedElementBinder<E> extends TypedBinder<E[]> {

		protected TypedElementBinder(Bind bind, Instance<E[]> instance) {
			super(bind.asMulti().next(), instance);
		}

		@SafeVarargs
		@SuppressWarnings("unchecked")
		public final void toElements(Class<? extends  E>... elems) {
			toElements(stream(elems) //
					.map(e -> relativeReferenceTo(raw(e))) //
					.toArray(Hint[]::new));
		}

		@SafeVarargs
		public final void toElements(Hint<? extends E>... elems) {
			expand(elems);
		}

		@SafeVarargs
		public final void toElements(E... constants) {
			to(array((Object[]) constants));
		}

		@SuppressWarnings("unchecked")
		private E[] array(Object... elements) {
			Class<E[]> rawType = getType().rawType;
			if (elements.getClass() == rawType)
				return (E[]) elements;
			Object[] res = newArray(getType().baseType().rawType,
					elements.length);
			System.arraycopy(elements, 0, res, 0, res.length);
			return (E[]) res;
		}
	}

	/**
	 * This cannot be changed to a lambda since we need a type that actually
	 * implements both {@link Supplier} and {@link Generator}. This way the
	 * {@link Generator} is picked directly by the {@link Injector}.
	 */
	private static final class SupplierGeneratorBridge<T>
			implements Supplier<T>, Generator<T> {

		private final Generator<T> generator;

		SupplierGeneratorBridge(Generator<T> generator) {
			this.generator = generator;
		}

		@Override
		public T generate(Dependency<? super T> dep)
				throws UnresolvableDependency {
			return generator.generate(dep);
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context)
				throws UnresolvableDependency {
			return generate(dep);
		}

	}
}
