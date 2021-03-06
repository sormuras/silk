package se.jbee.inject.bootstrap;

import se.jbee.inject.*;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.Toggled;
import se.jbee.inject.binder.BinderModule;

import java.util.function.Function;

import static se.jbee.inject.Cast.functionTypeOf;

/**
 * Capabilities of the basic {@link Injector} that can be installed or uninstalled.
 *
 * By default the {@link Bootstrap} utility will install all features.
 * Use {@link Bootstrapper#uninstall(Enum[])} to remove any unwanted capability.
 */
public enum InjectorFeature implements Toggled<InjectorFeature> {
	/**
	 * Binds a {@link Function} that given an array of {@link Class} (that must
	 * extend {@link Bundle}) creates or returns an {@link Injector} sub-context
	 * bootstrapped from the given set of classes.
	 */
	SUB_CONTEXT_FUNCTION,

	;

	@Override
	public void bootstrap(
			Bootstrapper.ToggledBootstrapper<InjectorFeature> bootstrapper) {
		bootstrapper.install(SubContextFunction.class, SUB_CONTEXT_FUNCTION);
	}

	static final class SubContextFunction extends BinderModule {

		@Override
		protected void declare() {
			Env env = env();
			asDefault().bind(functionTypeOf(Class[].class, Injector.class)) //
					.to(roots -> createSubContextFromRootBundles(env, roots));
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private Injector createSubContextFromRootBundles(Env env, Class[] roots) {
			return Bootstrap.injector(env, Bindings.newBindings(), roots);
		}
	}

}
