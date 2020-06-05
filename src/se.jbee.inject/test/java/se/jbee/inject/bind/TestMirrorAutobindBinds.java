package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Packages.packageAndSubPackagesOf;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.config.HintsBy.noParameters;
import static se.jbee.inject.config.NamesBy.defaultName;
import static se.jbee.inject.config.ProducesBy.allMethods;
import static se.jbee.inject.config.ProducesBy.declaredMethods;
import static se.jbee.inject.container.Cast.providerTypeOf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.util.Resource;
import se.jbee.inject.util.WebMethod;

/**
 * This test demonstrates the use of mirrors to semi-automatically bind
 * {@link Constructor}s and/or {@link Method}s as 'providers' of an instance.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestMirrorAutobindBinds {

	static final StringBuffer STATE = new StringBuffer();

	static class TestMirrorAutobindBindsModule extends BinderModule {

		@Override
		protected void declare() {
			autobind() //
					.produceBy(declaredMethods) //
					.in(this);
			autobind() //
					.produceBy(allMethods.annotatedWith(WebMethod.class)) //
					.nameBy(defaultName.unlessAnnotatedWith(Resource.class)) //
					.parameteriseBy(
							noParameters.unlessAnnotatedWith(Resource.class)) //
					.in(Implementor1.class);
			autobind() //
					.produceBy(allMethods.returnTypeAssignableTo(
							raw(Provider.class))) //
					.in(Implementor2.class);
			autobind() //
					.produceBy(allMethods.returnTypeIn(
							packageAndSubPackagesOf(Injector.class))) //
					.in(Implementor3.class);
			per(Scope.application).autobind() //
					.produceBy(declaredMethods) //
					.in(new Implementor4(STATE));
			per(Scope.injection).autobind() //
					.produceBy(declaredMethods) //
					.in(FactoryImpl.class);
		}

		static int staticFactoryMethod() {
			return 42;
		}

		static long staticFactoryMethodWithParameters(int factor) {
			return factor * 2L;
		}
	}

	static class FactoryImpl {

		byte i = 0;

		byte throwAwayInstanceFactoryMethod() {
			return i++;
		}
	}

	static class Implementor1 {

		@WebMethod
		float instanceFactoryMethod() {
			return 42f;
		}

		@WebMethod
		@Resource("twentyone")
		float instanceFactoryMethodWithName() {
			return 21f;
		}

		@WebMethod
		@Resource("Foo")
		double instanceFactoryMethodWithParameters(
				@Resource("twentyone") float factor) {
			return factor * 2d;
		}

		float shouldNotBeBoundSinceItIsNotAnnotated() {
			return 0f;
		}
	}

	static class Implementor2 {

		Provider<Boolean> assignableToProvider() {
			return () -> true;
		}

		Character shouldNotBeBoundSinceItDoesNotReturnTypeThatIsAssignableToProvider() {
			return 'N';
		}
	}

	static class Implementor3 {

		Name typeInProjectPackage() {
			return Name.named("foobar");
		}

		String shouldNotBeBoundSinceItDoesNotReturnTypeThatIsInProjectPackage() {
			return "foobar";
		}
	}

	static class Implementor4 {

		final StringBuffer state;

		Implementor4(StringBuffer state) {
			this.state = state;
		}

		StringBuffer valueFromStatefullMirrorObject() {
			return state;
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestMirrorAutobindBindsModule.class);

	@Test
	public void thatInstanceFactoryMethodIsAvailable() {
		assertEquals(42f, injector.resolve(float.class).floatValue(), 0.01f);
	}

	@Test
	public void thatStaticFactoryMethodIsAvailable() {
		assertEquals(42, injector.resolve(int.class).intValue());
	}

	/**
	 * Through the float parameter annotation {@link Resource} the double
	 * producing factory method gets 21f injected instead of 42f.
	 */
	@Test
	public void thatInstanceFactoryMethodWithParametersIsAvailable() {
		assertEquals(42d, injector.resolve(double.class), 0.01d);
	}

	@Test
	public void thatStaticFactoryMethodWithParametersIsAvailable() {
		assertEquals(84L, injector.resolve(long.class).longValue());
	}

	/**
	 * The provider method
	 * {@link Implementor1#instanceFactoryMethodWithParameters(float)} uses the
	 * {@link Resource} annotation to specify the name. As a result there is a
	 * named {@link Instance} that can be resolved.
	 */
	@Test
	public void thatNamedWithAnnotationCanBeUsedToGetNamedResources() {
		assertEquals(42d, injector.resolve("Foo", double.class), 0.01d);
	}

	@Test
	public void thatMethodsAreBoundThatAreAssignableToSpecifiedType() {
		assertEquals(true,
				injector.resolve(providerTypeOf(Boolean.class)).provide());
	}

	@Test(expected = NoResourceForDependency.class)
	public void thatNoMethodsAreBoundThatAreNotAssignableToSpecifiedType() {
		injector.resolve(Character.class);
	}

	@Test
	public void thatMethodsAreBoundThatAreInSpecifiedPackagesSet() {
		assertEquals(named("foobar"), injector.resolve(Name.class));
	}

	@Test(expected = NoResourceForDependency.class)
	public void thatNoMethodsAreBoundThatAreNotInSpecifiedPackagesSet() {
		injector.resolve(String.class);
	}

	@Test
	public void thatMethodsAreBoundToSpecificInstance() {
		assertSame(STATE, injector.resolve(StringBuffer.class));
	}

	@Test
	public void thatDeclaredScopeIsUsedForMirrorBindings() {
		byte first = injector.resolve(byte.class);
		byte second = injector.resolve(byte.class);
		assertEquals(first + 1, second);
		byte third = injector.resolve(byte.class);
		assertEquals(second + 1, third);
	}
}