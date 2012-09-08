package de.jbee.inject.util;

import de.jbee.inject.Type;

public final class Value<T> {

	public static <T> Value<T> value( Type<T> type, T value ) {
		return new Value<T>( type, value );
	}

	private final Type<T> type;
	private final T value;

	private Value( Type<T> type, T value ) {
		super();
		this.type = type;
		this.value = value;
	}

	public Type<T> getType() {
		return type;
	}

	public T getValue() {
		return value;
	}
}