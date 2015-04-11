/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.Array;
import se.jbee.inject.BindingIsInconsistent;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.DeclarationType;
import se.jbee.inject.Instance;
import se.jbee.inject.Resource;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.Typed;
import se.jbee.inject.container.Assembly;
import se.jbee.inject.container.Scope;

/**
 * Default data strature to represent a 4-tuple created from {@link Bindings}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of the bound value (instance)
 */
public final class Binding<T>
		implements Comparable<Binding<?>>, Assembly<T>, Module, Typed<T> {

	public static <T> Binding<T> binding( Resource<T> resource, BindingType type,
			Supplier<? extends T> supplier, Scope scope, Source source ) {
		return new Binding<T>( resource, type, supplier, scope, source );
	}

	public final Resource<T> resource;
	public final BindingType type;
	public final Supplier<? extends T> supplier;
	public final Scope scope;
	public final Source source;

	private Binding( Resource<T> resource, BindingType type, Supplier<? extends T> supplier,
			Scope scope, Source source ) {
		super();
		this.resource = resource;
		this.type = type;
		this.supplier = supplier;
		this.scope = scope;
		this.source = source;
	}

	@Override
	public Resource<T> resource() {
		return resource;
	}
	
	@Override
	public Scope scope() {
		return scope;
	}
	
	@Override
	public Source source() {
		return source;
	}
	
	@Override
	public Supplier<? extends T> supplier() {
		return supplier;
	}
	
	@Override
	public Type<T> type() {
		return resource.type();
	}

	@SuppressWarnings ( "unchecked" )
	@Override
	public <E> Binding<E> typed( Type<E> type ) {
		if ( !type().isAssignableTo( type ) ) {
			throw new ClassCastException(
					"New type of a binding has to be a assignable from :" + type() + " but was: " + type );
		}
		return new Binding<E>( resource.typed( type ), this.type, (Supplier<? extends E>) supplier,	scope, source );
	}

	public boolean isComplete() {
		return supplier != null;
	}
	
	public Binding<T> complete( BindingType type, Supplier<? extends T> supplier ) {
		return new Binding<T>( resource, type, supplier, scope, source );
	}

	@Override
	public void declare( Bindings bindings ) {
		bindings.add( this );
	}

	@Override
	public int compareTo( Binding<?> other ) {
		int res = resource.type().getRawType().getCanonicalName().compareTo(
				other.resource.type().getRawType().getCanonicalName() );
		if ( res != 0 ) {
			return res;
		}
		res = Instance.comparePrecision( resource.instance, other.resource.instance );
		if ( res != 0 ) {
			return res;
		}
		res = Instance.comparePrecision( resource.target, other.resource.target );
		if ( res != 0 ) {
			return res;
		}
		res = Instance.comparePrecision( source, other.source );
		if ( res != 0 ) {
			return res;
		}
		return -1; // keep order
	}

	@Override
	public String toString() {
		return resource + " / " + scope + " / " + source;
	}

	/**
	 * Removes those bindings that are ambiguous but also do not clash because of different
	 * {@link DeclarationType}s that replace each other.
	 */
	public static Binding<?>[] disambiguate( Binding<?>[] bindings ) {
		if ( bindings.length <= 1 ) {
			return bindings;
		}
		List<Binding<?>> uniques = new ArrayList<Binding<?>>( bindings.length );
		Arrays.sort( bindings );
		uniques.add( bindings[0] );
		int lastDistinctIndex = 0;
		Set<Type<?>> required = new HashSet<Type<?>>();
		Set<Type<?>> nullified = new HashSet<Type<?>>();
		for ( int i = 1; i < bindings.length; i++ ) {
			Binding<?> one = bindings[lastDistinctIndex];
			Binding<?> other = bindings[i];
			final boolean equalResource = one.resource.equalTo( other.resource );
			DeclarationType oneType = one.source.declarationType;
			DeclarationType otherType = other.source.declarationType;
			if ( equalResource && oneType.clashesWith( otherType ) ) {
				throw new BindingIsInconsistent( "Duplicate binds:\n" + one + "\n" + other );
			}
			if ( other.source.declarationType == DeclarationType.REQUIRED ) {
				required.add( other.resource.type() );
			} else if ( equalResource && oneType.nullifiedBy( otherType ) ) {
				if ( i - 1 == lastDistinctIndex ) {
					uniques.remove( uniques.size() - 1 );
					nullified.add( one.resource.type() );
				}
			} else if ( !equalResource || !otherType.replacedBy( oneType ) ) {
				uniques.add( other );
				lastDistinctIndex = i;
			}
		}
		if ( required.isEmpty() ) {
			return Array.of( uniques, Binding.class );
		}
		Set<Type<?>> bound = new HashSet<Type<?>>();
		Set<Type<?>> provided = new HashSet<Type<?>>();
		for ( Binding<?> b : uniques ) {
			Type<?> type = b.resource.type();
			if ( b.source.declarationType == DeclarationType.PROVIDED ) {
				provided.add( type );
			} else {
				bound.add( type );
			}
		}
		required.removeAll( bound );
		if ( !provided.containsAll( required ) ) {
			required.removeAll( provided );
			throw new UnresolvableDependency.NoResourceForDependency( required );
		}
		List<Binding<?>> res = new ArrayList<Binding<?>>( uniques.size() );
		for ( int i = 0; i < uniques.size(); i++ ) {
			Binding<?> b = uniques.get( i );
			if ( b.source.declarationType != DeclarationType.PROVIDED
					|| required.contains( b.resource.type() ) ) {
				res.add( b );
			}
		}
		return Array.of( res, Binding.class );
	}

}