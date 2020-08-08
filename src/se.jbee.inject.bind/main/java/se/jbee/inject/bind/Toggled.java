/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Env;
import se.jbee.inject.bind.Bootstrapper.ToggledBootstrapper;

/**
 * A {@link Bundle} that does installs {@link Bundle}s in connected to a feature
 * flag represented by an {@link Enum} constant. If the flag is
 * {@link Env#toggled(Class, Enum)} the {@link Bundle} is installed,
 * otherwise it isn't.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@FunctionalInterface
public interface Toggled<F> {

	/**
	 * @param bootstrapper the {@link ToggledBootstrapper} this bundle should
	 *            install itself in.
	 */
	void bootstrap(ToggledBootstrapper<F> bootstrapper);
}
