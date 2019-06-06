/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 * @author sarahferry
 * @author alexscheitlin
 */
public class UserNameValidatorTest {

    @ParameterizedTest
    @MethodSource("inputsForTestIsEmailValid")
    public void testIsUserNameValid(boolean isValid, String userName) {
        assertEquals(isValid, UserNameValidator.isUserNameValid(userName, null));
    }

    // arguments for parameterized test - testIsUserNameValid
    private static Stream<Arguments> inputsForTestIsEmailValid() {
        return Stream.of(
            // good usernames
            Arguments.of( true, "sarah" ),
            Arguments.of( true, ".-_5Arah_-." ),

            // dont allow accents
            Arguments.of( false, "àèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝâêîôûÂÊÎÔÛãñõÃÑÕäëïöüÿÄËÏÖÜŸçÇßØøÅåÆæœ" ),

            // dont allow chinese characters
            Arguments.of( false, "谁日吧爸好" ),

            // dont allow middle white space
            Arguments.of( false, "sarah f" ),

            // dont allow leading white space
            Arguments.of( false, " sarah" ),

            // dont allow trailing white space
            Arguments.of( false, "sarah " ),

            // dont allow symbols
            Arguments.of( false, "sarah!" ),
            Arguments.of( false, "sarah?" ),
            Arguments.of( false, "sarah:(" ),
            Arguments.of( false, "💲🅰️®️🅰️🚧" ),

            // only allow between 2 and 60 characters
            Arguments.of( false, "q" ),
            Arguments.of( true, "q2" ),
            Arguments.of( false, "q2jsalfhjopiwurtiosfhkdhasjkdhfgkfhkfrhnefcn4cqonroclmooi4oiqwhrfq4jrlqhaskdalwehrlwhflhklasdjfglq0kkajfelirhilwhakjgv" ),
            Arguments.of( false, "" ),
            Arguments.of( false, null )
        );
    }
}
