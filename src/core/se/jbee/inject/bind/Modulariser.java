/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

/**
 * Determines / extracts the {@link Module} result from a root {@link Bundle}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Modulariser {

	/**
	 * @return All {@link Module} that result from expanding the given root {@link Bundle} to the
	 *         module level.
	 */
	Module[] modularise( Class<? extends Bundle> root );
}
