/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.accessible;
import static se.jbee.inject.Utils.arrayOf;
import static se.jbee.inject.config.ConstructionMirror.noArgsConstructor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.jbee.inject.InconsistentBinding;
import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.config.Choices;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Options;
import se.jbee.inject.container.Container;

/**
 * Utility to create an {@link Injector} context from {@link Bundle}s and
 * {@link Module}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bootstrap {

	public static Injector injector(Class<? extends Bundle> root) {
		return injector(root, Globals.STANDARD);
	}

	public static Injector injector(Class<? extends Bundle> root,
			Globals globals) {
		return injector(root, Bindings.newBindings(), globals);
	}

	public static Injector injector(Class<? extends Bundle> root,
			Bindings bindings, Globals globals) {
		return injector(bindings, modulariser(globals).modularise(root));
	}

	public static Injector injector(Bindings bindings, Module[] modules) {
		return Container.injector(
				Binding.disambiguate(bindings.declareFrom(modules)));
	}

	public static Modulariser modulariser(Globals globals) {
		return new BuildinBootstrapper(globals);
	}

	public static Bundler bundler(Globals globals) {
		return new BuildinBootstrapper(globals);
	}

	public static Binding<?>[] bindings(Class<? extends Bundle> root,
			Bindings bindings, Globals globals) {
		return Binding.disambiguate(
				bindings.declareFrom(modulariser(globals).modularise(root)));
	}

	public static <T> Module module(ModuleWith<T> module, Options presets) {
		return new PresetModuleBridge<>(module, presets);
	}

	public static void nonnullThrowsReentranceException(Object field) {
		if (field != null)
			throw new InconsistentBinding("Reentrance not allowed!");
	}

	public static <T> T instance(Class<T> type) {
		return Supply.constructor(accessible(noArgsConstructor(type)));
	}

	private Bootstrap() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * Implements the {@link ModuleWith} abstraction by presenting them as
	 * {@link Module}.
	 * 
	 * @param <T> type of the {@link Options} value injected into the
	 *            {@link ModuleWith}
	 */
	private static final class PresetModuleBridge<T> implements Module {

		private final ModuleWith<T> module;
		private final Options presets;

		PresetModuleBridge(ModuleWith<T> module, Options presets) {
			this.module = module;
			this.presets = presets;
		}

		@Override
		public void declare(Bindings bindings) {
			Type<?> valueType = Type.supertype(ModuleWith.class,
					raw(module.getClass())).parameter(0);
			@SuppressWarnings("unchecked")
			final T value = (T) (valueType.rawType == Options.class
				? presets
				: presets.get(valueType));
			module.declare(bindings, value);
		}
	}

	private static final class BuildinBootstrapper
			implements Bootstrapper, Bundler, Modulariser {

		private final Map<Class<? extends Bundle>, Set<Class<? extends Bundle>>> bundleChildren = new IdentityHashMap<>();
		private final Map<Class<? extends Bundle>, List<Module>> bundleModules = new IdentityHashMap<>();
		private final Set<Class<? extends Bundle>> uninstalled = new HashSet<>();
		private final Set<Class<? extends Bundle>> installed = new HashSet<>();
		private final LinkedList<Class<? extends Bundle>> stack = new LinkedList<>();
		private final Globals globals;

		BuildinBootstrapper(Globals globals) {
			this.globals = globals;
		}

		@Override
		public void install(Class<? extends Bundle> bundle) {
			if (uninstalled.contains(bundle) || installed.contains(bundle))
				return;
			if (!globals.edition.featured(bundle)) {
				// this way we will never ask again - something not featured is finally not featured
				uninstalled.add(bundle);
				return;
			}
			installed.add(bundle);
			if (!stack.isEmpty()) {
				final Class<? extends Bundle> parent = stack.peek();
				Set<Class<? extends Bundle>> children = bundleChildren.get(
						parent);
				if (children == null) {
					children = new LinkedHashSet<>();
					bundleChildren.put(parent, children);
				}
				children.add(bundle);
			}
			stack.push(bundle);
			Bootstrap.instance(bundle).bootstrap(this);
			if (stack.pop() != bundle)
				throw new IllegalStateException(bundle.getCanonicalName());
		}

		@Override
		public <C extends Enum<C>> void install(
				Class<? extends ChoiceBundle<C>> bundle,
				final Class<C> property) {
			if (!globals.edition.featured(property))
				return;
			final Choices choices = globals.choices;
			Bootstrap.instance(bundle).bootstrap((bundleForChoice, choice) -> {
				// NB: null is a valid value to define what happens when no configuration is present
				if (choices.isChosen(property, choice)) {
					BuildinBootstrapper.this.install(bundleForChoice);
				}
			});
		}

		@Override
		@SafeVarargs
		public final <C extends Enum<C> & ChoiceBundle<C>> void install(
				C... choices) {
			if (choices.length > 0) {
				final C choice0 = choices[0];
				if (!globals.edition.featured(choice0.getClass()))
					return;
				final EnumSet<C> installing = EnumSet.of(choice0, choices);
				choice0.bootstrap((bundle, onOption) -> {
					if (installing.contains(onOption))
						BuildinBootstrapper.this.install(bundle);
				});
			}
		}

		@Override
		public void install(Module module) {
			Class<? extends Bundle> bundle = stack.peek();
			if (uninstalled.contains(bundle)
				|| !globals.edition.featured(module.getClass()))
				return;
			bundleModules.computeIfAbsent(bundle, __ -> new ArrayList<>()).add(
					module);
		}

		@Override
		public <T> void install(ModuleWith<T> module) {
			install(module(module, globals.options));
		}

		@Override
		public Module[] modularise(Class<? extends Bundle> root) {
			return modulesOf(bundle(root));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Class<? extends Bundle>[] bundle(Class<? extends Bundle> root) {
			if (!installed.contains(root))
				install(root);
			Set<Class<? extends Bundle>> installed = new LinkedHashSet<>();
			addAllInstalledIn(root, installed);
			return arrayOf(installed, Class.class);
		}

		private Module[] modulesOf(Class<? extends Bundle>[] bundles) {
			List<Module> installed = new ArrayList<>(bundles.length);
			for (Class<? extends Bundle> b : bundles) {
				List<Module> modules = bundleModules.get(b);
				if (modules != null)
					installed.addAll(modules);
			}
			return arrayOf(installed, Module.class);
		}

		@Override
		public void uninstall(Class<? extends Bundle> bundle) {
			if (uninstalled.contains(bundle))
				return;
			uninstalled.add(bundle);
			installed.remove(bundle);
			for (Set<Class<? extends Bundle>> c : bundleChildren.values())
				c.remove(bundle);
			bundleModules.remove(bundle); // we are sure we don't need its modules
		}

		@Override
		@SafeVarargs
		public final <O extends Enum<O> & ChoiceBundle<O>> void uninstall(
				O... bundles) {
			if (bundles.length > 0) {
				final EnumSet<O> uninstalling = EnumSet.of(bundles[0], bundles);
				bundles[0].bootstrap((bundle, choice) -> {
					if (uninstalling.contains(choice))
						uninstall(bundle);
				});
			}
		}

		private void addAllInstalledIn(Class<? extends Bundle> bundle,
				Set<Class<? extends Bundle>> accu) {
			accu.add(bundle);
			Set<Class<? extends Bundle>> children = bundleChildren.get(bundle);
			if (children == null)
				return;
			for (Class<? extends Bundle> c : children)
				if (!accu.contains(c))
					addAllInstalledIn(c, accu);
		}

	}
}
