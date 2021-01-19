package com.ahli.galaxy.ui;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class UIConstantTest {
	@Test
	@SuppressWarnings("JUnitTestMethodWithNoAssertions")
	void equalsContract() {
		EqualsVerifier.forClass(UIConstant.class).withRedefinedSuperclass().suppress(Warning.NONFINAL_FIELDS).verify();
	}
}
