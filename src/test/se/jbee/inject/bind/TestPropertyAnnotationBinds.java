package se.jbee.inject.bind;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.parameterType;
import static se.jbee.inject.Type.raw;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Parameter;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Env;
import se.jbee.inject.config.Environment;
import se.jbee.inject.config.HintsBy;
import se.jbee.inject.container.Supplier;

/**
 * This test illustrates how to extends the {@link Injector} so it does inject
 * {@link String} fields with a custom annotation referencing a certain property
 * value.
 */
public class TestPropertyAnnotationBinds {

	/**
	 * A custom annotation used to mark {@link String} parameters that should be
	 * injected with a certain property whose name is given by the annotation
	 * value.
	 */
	@Retention(RUNTIME)
	@Target({ PARAMETER, FIELD })
	@interface Property {
		String value();
	}

	static class TestPropertyAnnotationBindsModule
			extends BinderModuleWith<Properties> {

		/**
		 * Configures custom {@link HintsBy}.
		 */
		@Override
		protected Env configure(Env env) {
			return Environment.override(env).with(
					HintsBy.class, this::hints);
		}

		/**
		 * The custom {@link HintsBy} implementation checks the
		 * the {@link Property} annotation; if present, a {@link Instance} hint
		 * is added to the constructor arguments linking the parameter to a
		 * named {@link String}. The name of that {@link String} is derived from
		 * the annotation value. The added {@link PropertySupplier} will be
		 * called when resolving the namespaced {@link String}. It extracts the
		 * property from the name and resolves it from the properties.
		 */
		private Parameter<?>[] hints(Executable obj) {
			List<Parameter<?>> hints = new ArrayList<>();
			for (java.lang.reflect.Parameter param : obj.getParameters()) {
				if (param.isAnnotationPresent(Property.class)) {
					Name name = named(Property.class).concat(
							param.getAnnotation(Property.class).value());
					Instance<?> hint = instance(name, parameterType(param));
					hints.add(hint);
				}
			}
			return hints.isEmpty()
				? Parameter.noParameters
				: hints.toArray(new Parameter[0]);
		}

		@Override
		protected void declare(Properties properties) {
			bind(Properties.class).to(properties);
			per(Scope.dependencyInstance).bind(named(Property.class).asPrefix(),
					raw(String.class)).toSupplier(new PropertySupplier());
			// just to test
			bind(ExampleBean.class).toConstructor();
		}

	}

	static final class PropertySupplier implements Supplier<String> {

		@Override
		public String supply(Dependency<? super String> dep, Injector context)
				throws UnresolvableDependency {
			return context.resolve(Properties.class) //
					.getProperty(dep.instance.name.withoutNamespace());
		}
	}

	static class ExampleBean {

		final String property1;
		final String property2;

		ExampleBean(@Property("foo") String property1,
				@Property("bar") String property2) {
			this.property1 = property1;
			this.property2 = property2;
		}
	}

	@Test
	public void customMirrorAndSupplierCanBeUsedToAddCustomAnnotation() {
		Properties properties = new Properties();
		properties.put("foo", "property1");
		properties.put("bar", "property2");
		Env env = Bootstrap.ENV.with(Properties.class, properties);
		Injector injector = Bootstrap.injector(
				TestPropertyAnnotationBindsModule.class, env);
		ExampleBean bean = injector.resolve(ExampleBean.class);
		assertEquals("property1", bean.property1);
		assertEquals("property2", bean.property2);
	}
}