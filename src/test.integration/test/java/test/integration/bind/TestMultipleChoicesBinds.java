package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.binder.TogglerBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Environment;

import static test.integration.bind.AssertInjects.assertEqualSets;

public class TestMultipleChoicesBinds {

	private enum Text {
		A, B, C, D, E
	}

	private static class A extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("A");
		}
	}

	private static class B extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("B");
		}
	}

	private static class C extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("C");
		}
	}

	private static class D extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("D");
		}
	}

	private static class E extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("E");
		}
	}

	private static class ToggledBundle extends TogglerBundle<Text> {

		@Override
		protected void bootstrap() {
			install(A.class, Text.A);
			install(B.class, Text.B);
			install(C.class, Text.C);
			install(D.class, Text.D);
			install(E.class, Text.E);
		}
	}

	private static class RootBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(ToggledBundle.class, Text.class);
		}
	}

	@Test
	public void thatMultipleChoicesArePossible() {
		Env env = Environment.DEFAULT.withToggled(Text.class, Text.A, Text.D);
		Injector injector = Bootstrap.injector(env, RootBundle.class);
		assertEqualSets(new String[] { "A", "D" },
				injector.resolve(String[].class));
	}
}
