package se.jbee.inject.defaults;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.Scope.container;
import static se.jbee.inject.ScopePermanence.singleton;
import static se.jbee.inject.ScopePermanence.unstable;
import static se.jbee.inject.scope.DiskScope.SYNC_INTERVAL;
import static se.jbee.inject.scope.DiskScope.SYNC_INTERVAL_DEFAULT_DURATION;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.ScopePermanence;
import se.jbee.inject.Supplier;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bind.Bind;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.config.Config;
import se.jbee.inject.scope.ApplicationScope;
import se.jbee.inject.scope.DiskScope;
import se.jbee.inject.scope.ThreadScope;
import se.jbee.inject.scope.TypeDependentScope;
import se.jbee.inject.scope.WorkerScope;

/**
 * Binds implementations for the standard {@link Scope}s declared as
 * {@link Name} in the {@link Scope} class.
 * 
 * This includes {@link DiskScope}s that use their root folder as part of the
 * {@link Scope}'s {@link Name}.
 */
public final class DefaultScopes extends BinderModule
		implements Supplier<Scope> {

	@Override
	protected Bind init(Bind bind) {
		return bind.asDefault();
	}

	@Override
	protected void declare() {
		bindScopePermanence(ScopePermanence.reference);
		bindScopePermanence(ScopePermanence.container);
		bindScopePermanence(singleton.derive(Scope.jvm));
		bindScopePermanence(singleton.derive(Scope.application));
		bindScopePermanence(singleton.derive(Scope.dependency));
		bindScopePermanence(singleton.derive(Scope.dependencyType));
		bindScopePermanence(singleton.derive(Scope.dependencyInstance));
		bindScopePermanence(singleton.derive(Scope.targetInstance));
		bindScopePermanence(unstable.derive(Scope.thread) //
				.canBeInjectedInto(Scope.thread)//
				.canBeInjectedInto(Scope.worker) //
				.canBeInjectedInto(Scope.injection)); //
		bindScopePermanence(unstable.derive(Scope.injection) //
				.canBeInjectedInto(Scope.injection));
		bindScopePermanence(unstable.derive(Scope.worker) //
				.canBeInjectedInto(Scope.worker) //
				.canBeInjectedInto(Scope.injection)); //

		bindScope(Scope.injection).to(Scope.INJECTION);
		bindScope(Scope.application).to(ApplicationScope.class);
		bindScope(Scope.thread).to(ThreadScope.class);
		bindScope(Scope.jvm).to(TypeDependentScope.JVM);
		bindScope(Scope.worker).to(WorkerScope.class);
		per(Scope.worker).bind(
				Scope.Controller.forScope(Scope.worker)).toGenerator(
						gen -> null); // dummy generator as the scope will supply

		bindScope(Scope.dependency).to(() -> new TypeDependentScope(
				TypeDependentScope::hierarchicalInstanceSignature));
		bindScope(Scope.dependencyInstance).to(() -> new TypeDependentScope(
				TypeDependentScope::instanceSignature));
		bindScope(Scope.dependencyType).to(() -> new TypeDependentScope(
				TypeDependentScope::typeSignature));
		bindScope(Scope.targetInstance).to(() -> new TypeDependentScope(
				TypeDependentScope::targetInstanceSignature));

		bindScope(named("disk:*")).toSupplier(this);
		per(container).bind(ScheduledExecutorService.class).to(
				Executors::newSingleThreadScheduledExecutor);
	}

	/**
	 * By convention {@link Scope#disk(File)} create names starting with
	 * {@code disk:} followed by the path to the folder of the scope. This can
	 * be used to extract the directory in this {@link Supplier} to bind the
	 * particular {@link DiskScope} for that directory.
	 */
	@Override
	public Scope supply(Dependency<? super Scope> dep, Injector context)
			throws UnresolvableDependency {
		String disk = dep.instance.name.toString();
		File dir = new File(disk.substring(5));
		long syncInterval = context.resolve(Config.class) //
				.longValue(SYNC_INTERVAL, SYNC_INTERVAL_DEFAULT_DURATION);
		return new DiskScope(syncInterval,
				context.resolve(ScheduledExecutorService.class), dir,
				TypeDependentScope::instanceSignature);
	}

}