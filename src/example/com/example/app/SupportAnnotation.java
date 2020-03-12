package com.example.app;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import se.jbee.inject.Link;
import se.jbee.inject.Scope;
import se.jbee.inject.bind.BinderModuleWith;

/**
 * In contrast to {@link ServiceAnnotation} this will be bound to the annotation
 * using {@link ServiceLoader}.
 * 
 * There are two ways to link this class to an {@link Annotation}.
 * 
 * 1. Annotated this class with the {@link Annotation} it implements (works only
 * if there is no other annotation present)
 * 
 * 2. Use {@link Link} annotation to point out the {@link Annotation}
 * {@link Class} to link this class with
 * 
 * As {@link Link} allows other annotations to be present it should be preferred
 * where possible.
 */
@Link(to = Support.class)
public class SupportAnnotation extends BinderModuleWith<Class<?>> {

	@Override
	protected void declare(Class<?> annotated) {
		per(Scope.application).autobind(annotated).toConstructor();
	}
}