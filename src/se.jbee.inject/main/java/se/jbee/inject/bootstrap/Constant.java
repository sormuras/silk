/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import se.jbee.inject.declare.ValueBinder;

/**
 * A {@link Constant} is the {@link ValueBinder} expansion wrapper type for any
 * constant bound to in the fluent binder API.
 * 
 * @param <T> Type of the constant value
 */
public final class Constant<T> {

	public final T value;
	public final boolean autoBindExactType;

	public Constant(T value) {
		this(value, true);
	}

	public Constant(T value, boolean autoBindExactType) {
		this.value = value;
		this.autoBindExactType = autoBindExactType;
	}

}