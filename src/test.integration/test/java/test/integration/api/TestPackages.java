package test.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Packages.packageOf;
import static se.jbee.inject.lang.Type.raw;

import java.text.Format;
import java.text.spi.DateFormatProvider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import se.jbee.inject.Packages;
import se.jbee.inject.lang.Type;

public class TestPackages {

	@Test
	public void thatLowerBoundTypeIsNotInPackageJavaLang() {
		Packages javaLang = packageOf(String.class);
		assertFalse(javaLang.contains(Type.WILDCARD));
		assertFalse(javaLang.contains(raw(List.class).asUpperBound()));
	}

	@Test
	public void thatPackageAllContainsAllTypes() {
		Packages all = Packages.ALL;
		assertTrue(all.contains(raw(List.class)));
		assertTrue(all.contains(raw(AtomicBoolean.class)));
		assertTrue(all.contains(raw(List.class).asUpperBound()));
	}

	@Test
	public void thatPackageContainsType() {
		Packages javaUtil = Packages.packageOf(String.class);
		assertTrue(javaUtil.contains(raw(String.class)));
		assertFalse(javaUtil.contains(raw(ConcurrentLinkedQueue.class)));
		assertFalse(javaUtil.contains(raw(AtomicBoolean.class)));
		assertFalse(javaUtil.contains(raw(Format.class)));
	}

	@Test
	public void thatSubpackagesContainType() {
		Packages javaUtil = Packages.subPackagesOf(List.class);
		assertFalse(javaUtil.contains(raw(List.class)));
		assertTrue(javaUtil.contains(raw(ConcurrentLinkedQueue.class)));
		assertTrue(javaUtil.contains(raw(AtomicBoolean.class)));
		assertFalse(javaUtil.contains(raw(Format.class)));
	}

	@Test
	public void thatPackgeAndSubpackagesContainType() {
		Packages javaUtil = Packages.packageAndSubPackagesOf(List.class);
		assertTrue(javaUtil.contains(raw(List.class)));
		assertTrue(javaUtil.contains(raw(ConcurrentLinkedQueue.class)));
		assertTrue(javaUtil.contains(raw(AtomicBoolean.class)));
		assertFalse(javaUtil.contains(raw(Format.class)));
	}

	@Test
	public void thatIndividualPackagesCanBeCherryPicked() {
		Packages cherries = Packages.packageOf(List.class, AtomicBoolean.class,
				String.class);
		assertTrue(cherries.contains(raw(List.class)));
		assertTrue(cherries.contains(raw(AtomicBoolean.class)));
		assertTrue(cherries.contains(raw(String.class)));
		assertTrue(cherries.contains(raw(Long.class)));
		assertFalse(cherries.contains(raw(Format.class)));
	}

	@Test
	public void thatMultipleRootSubpackagesOfSameDepthCanBeCombined() {
		Packages subs = Packages.subPackagesOf(List.class, Format.class);
		assertFalse(subs.contains(raw(List.class))); // in java.util
		assertTrue(subs.contains(raw(AtomicBoolean.class))); // in java.uitl.concurrent
		assertFalse(subs.contains(raw(Format.class))); // in java.text
		assertTrue(subs.contains(raw(DateFormatProvider.class))); // in java.text.spi
	}

	@Test
	public void thatMultipleRootPackagesAndSubpackagesOfSameDepthCanBeCombined() {
		Packages subs = Packages.packageAndSubPackagesOf(List.class,
				Format.class);
		assertTrue(subs.contains(raw(List.class))); // in java.util
		assertTrue(subs.contains(raw(AtomicBoolean.class))); // in java.uitl.concurrent
		assertTrue(subs.contains(raw(Format.class))); // in java.text
		assertTrue(subs.contains(raw(DateFormatProvider.class))); // in java.text.spi
	}

	@Test(expected = IllegalArgumentException.class)
	public void thatMultipleRootSubpackagesOfDifferentDepthCanNotBeCombined() {
		Packages.subPackagesOf(List.class, DateFormatProvider.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void thatMultipleRootPackagesAndSubpackagesOfDifferentDepthCanNotBeCombined() {
		Packages.packageAndSubPackagesOf(List.class, DateFormatProvider.class);
	}

	@Test
	public void thatParentPackagesAreOfSameKindOfSet() {
		assertEquals(Packages.subPackagesOf(Map.class),
				Packages.subPackagesOf(ConcurrentMap.class).parents());
		assertEquals(Packages.packageOf(Map.class),
				Packages.packageOf(ConcurrentMap.class).parents());
		assertEquals(Packages.packageAndSubPackagesOf(Map.class),
				Packages.packageAndSubPackagesOf(
						ConcurrentMap.class).parents());
	}

	@Test
	public void thatParentOfAllPackagesIsAllPackages() {
		assertEquals(Packages.ALL, Packages.ALL.parents());
	}

	@Test
	public void thatParentOfDefaultPackageIsDefaultPackage() {
		assertEquals(Packages.DEFAULT, Packages.DEFAULT.parents());
	}
}
