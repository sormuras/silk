/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

/**
 * The default utility {@link Bundle} that is a {@link Bootstrap} as well so
 * that bindings can be declared nicer.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BootstrapperBundle implements Bundle, Bootstrapper {

	private Bootstrapper bootstrap;

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		Bootstrap.nonnullThrowsReentranceException(this.bootstrap);
		this.bootstrap = bootstrap;
		bootstrap();
	}

	@Override
	public final void install(Class<? extends Bundle> bundle) {
		bootstrap.install(bundle);
	}

	@Override
	public final void install(Module module) {
		bootstrap.install(module);
	}

	@Override
	public final <T> void install(ModuleWith<T> module) {
		bootstrap.install(module);
	}

	@Override
	public final void uninstall(Class<? extends Bundle> bundle) {
		bootstrap.uninstall(bundle);
	}

	@Override
	@SafeVarargs
	public final <M extends Enum<M> & ChoiceBundle<M>> void install(
			M... modules) {
		bootstrap.install(modules);
	}

	@Override
	public final <C extends Enum<C>> void install(
			Class<? extends ChoiceBundle<C>> bundle, Class<C> property) {
		bootstrap.install(bundle, property);
	}

	@Override
	@SafeVarargs
	public final <O extends Enum<O> & ChoiceBundle<O>> void uninstall(
			O... options) {
		bootstrap.uninstall(options);
	}

	protected final <O extends Enum<O> & ChoiceBundle<O>> void installAll(
			Class<O> optionsOfType) {
		install(optionsOfType.getEnumConstants());
	}

	protected final <O extends Enum<O> & ChoiceBundle<O>> void uninstallAll(
			Class<O> optionsOfType) {
		uninstall(optionsOfType.getEnumConstants());
	}

	protected static Module newInstance(Class<? extends Module> module) {
		return Bootstrap.instance(module);
	}

	@Override
	public String toString() {
		return "bundle " + getClass().getSimpleName();
	}

	protected abstract void bootstrap();

}
