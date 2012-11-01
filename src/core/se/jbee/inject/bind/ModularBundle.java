/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.bind.Bootstrapper.ModularBootstrapper;

/**
 * A {@link Bundle} that does different installation for different options.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public interface ModularBundle<M> {

	/**
	 * @param bootstrap
	 *            the {@link ModularBootstrapper} this bundle should install itself in.
	 */
	void bootstrap( ModularBootstrapper<M> bootstrap );
}
