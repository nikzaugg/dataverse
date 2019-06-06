package edu.harvard.iq.dataverse.util;

import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class JsfHelperTest {

	enum TestEnum {
		Lorem, Ipsum, Dolor, Sit, Amet
	}

	@ParameterizedTest
	@MethodSource("inputsForTestEnumValue")
	public void testEnumValue(TestEnum inputEnum, String inputString, TestEnum defaultEnumValue) {
		JsfHelper instance = new JsfHelper();
		assertEquals(inputEnum, instance.enumValue(inputString, TestEnum.class, defaultEnumValue));
	}

	private static Stream<Arguments> inputsForTestEnumValue() {
		return Stream.of(
				Arguments.of(TestEnum.Lorem, "Lorem", TestEnum.Dolor),
				Arguments.of(TestEnum.Lorem, "Lorem   ", TestEnum.Dolor), 
				Arguments.of(TestEnum.Dolor, null, TestEnum.Dolor),
				Arguments.of(TestEnum.Dolor, "THIS IS A BAD VALUE", TestEnum.Dolor)
				);
	}

}
