package de.jbee.inject.util;

import static de.jbee.inject.Type.raw;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.jbee.inject.Injectron;
import de.jbee.inject.Type;

/**
 * 
 * Implementation Note: storing the the raw type in a var before returning the generic type is a
 * workaround to make this compile with javac (cast works within eclipse).
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Typecast {

	public static <T> Type<? extends List<T>> listTypeOf( Class<T> elementType ) {
		return listTypeOf( raw( elementType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends List<T>> listTypeOf( Type<T> elementType ) {
		Type raw = raw( List.class ).parametized( elementType );
		return raw;
	}

	public static <T> Type<? extends Set<T>> setTypeOf( Class<T> elementType ) {
		return setTypeOf( raw( elementType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Set<T>> setTypeOf( Type<T> elementType ) {
		Type raw = raw( Set.class ).parametized( elementType );
		return raw;
	}

	public static <T> Type<? extends Collection<T>> collectionTypeOf( Class<T> elementType ) {
		return collectionTypeOf( raw( elementType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Collection<T>> collectionTypeOf( Type<T> elementType ) {
		Type raw = raw( Collection.class ).parametized( elementType );
		return raw;
	}

	public static <T> Type<? extends Provider<T>> providerTypeOf( Class<T> providedType ) {
		return providerTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Provider<T>> providerTypeOf( Type<T> providedType ) {
		Type raw = raw( Provider.class ).parametized( providedType );
		return raw;
	}

	public static <T> Type<? extends Factory<T>> factoryTypeOf( Class<T> providedType ) {
		return factoryTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Factory<T>> factoryTypeOf( Type<T> providedType ) {
		Type raw = raw( Factory.class ).parametized( providedType );
		return raw;
	}

	public static <T> Type<? extends Injectron<T>> injectronTypeOf( Class<T> providedType ) {
		return injectronTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Injectron<T>> injectronTypeOf( Type<T> providedType ) {
		Type raw = raw( Injectron.class ).parametized( providedType );
		return raw;
	}

	public static <T> Type<? extends Injectron<T>[]> injectronsTypeOf( Class<T> providedType ) {
		return injectronsTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Injectron<T>[]> injectronsTypeOf( Type<T> providedType ) {
		Type raw = raw( Injectron[].class ).parametized( providedType );
		return raw;
	}
}
