package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency.UnstableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.Assert.fail;

public class TestScopedBinds {

	private static class Foo {

		@SuppressWarnings("unused")
		Foo(Bar bar) {
			// it is just about the instances
		}
	}

	private static class Bar {
		// just to demo
	}

	private static class ScopedBindsModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.application).construct(Foo.class);
			per(Scope.injection).construct(Bar.class);
		}
	}

	@Test(expected = UnstableDependency.class)
	public void thatInjectingAnInjectionScopedInstanceIntoAppScopedInstanceThrowsAnException() {
		Injector injector = Bootstrap.injector(ScopedBindsModule.class);
		Foo foo = injector.resolve(Foo.class);
		fail("It should not be possible to create a foo but got one: " + foo);
	}

}
