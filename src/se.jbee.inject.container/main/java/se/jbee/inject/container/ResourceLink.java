package se.jbee.inject.container;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.Supplier;

/**
 * The {@link ResourceLink} is an abstraction to the internals of a
 * {@link Injector} implementation that {@link Generator} use when they actually
 * create an instance using the {@link Supplier} which should be called with the
 * actual {@link Injector} instance which might not be known when the
 * {@link Generator} was created. In a way this is necessary to solve a hen-egg
 * situation in the collaboration between {@link Generator} and {@link Injector}
 * context implementations.
 * 
 * It is also a bridge between the {@link Generator} on the user side and the
 * {@link Supplier} used when binding.
 *
 * @since 19.1
 */
@FunctionalInterface
public interface ResourceLink {

	/**
	 * 
	 * @param <T> type of the instance create.
	 * @param injected the {@link Dependency} currently resolved/injected
	 * @param supplier the source used to supply the returned instance
	 * @param resource the {@link Resource} used. Note that the instance cannot
	 *            be supplied using {@link Resource#generate(Dependency)} or any
	 *            other method using the {@link Resource#generator} as that
	 *            generator internally is calling this method. Doing so would
	 *            result in an endless loop.
	 * @return the instance supplied by the provided {@link Supplier}
	 */
	<T> T supplyInContext(Dependency<? super T> injected,
			Supplier<? extends T> supplier, Resource<T> resource);
}
