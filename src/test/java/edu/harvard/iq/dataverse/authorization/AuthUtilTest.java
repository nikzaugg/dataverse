package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GitHubOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.GoogleOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.Assert.*;

public class AuthUtilTest {

    @ParameterizedTest
    @MethodSource("inputsForTestGetDisplayName")
    public void testGetDisplayName(String expectedDisplayName, String displayFirst, String displayLast) {
        assertEquals(expectedDisplayName, AuthUtil.getDisplayName(displayFirst, displayLast));
    }

    // arguments for parameterized test - testGetDisplayName
    private static Stream<Arguments> inputsForTestGetDisplayName() {
        return Stream.of(
            Arguments.of(null, null, null), 
            Arguments.of("Homer", "Homer", null),
            Arguments.of("Simpson", null, "Simpson"), 
            Arguments.of("Homer Simpson", "Homer", "Simpson"),
            Arguments.of("Homer Simpson", " Homer", "Simpson")
        );
    }

    /**
     * Test of isNonLocalLoginEnabled method, of class AuthUtil.
     */
    @Test
    public void testIsNonLocalLoginEnabled() {
        System.out.println("isNonLocalLoginEnabled");

        assertEquals(false, AuthUtil.isNonLocalLoginEnabled(null));

        Collection<AuthenticationProvider> shibOnly = new HashSet<>();
        shibOnly.add(new ShibAuthenticationProvider());
        assertEquals(true, AuthUtil.isNonLocalLoginEnabled(shibOnly));

        Collection<AuthenticationProvider> manyNonLocal = new HashSet<>();
        manyNonLocal.add(new ShibAuthenticationProvider());
        manyNonLocal.add(new GitHubOAuth2AP(null, null));
        manyNonLocal.add(new GoogleOAuth2AP(null, null));
        manyNonLocal.add(new OrcidOAuth2AP(null, null, null));
        assertEquals(true, AuthUtil.isNonLocalLoginEnabled(manyNonLocal));

        Collection<AuthenticationProvider> onlyBuiltin = new HashSet<>();
        onlyBuiltin.add(new BuiltinAuthenticationProvider(null, null, null));
        // only builtin provider
        assertEquals(false, AuthUtil.isNonLocalLoginEnabled(onlyBuiltin));

    }
}
