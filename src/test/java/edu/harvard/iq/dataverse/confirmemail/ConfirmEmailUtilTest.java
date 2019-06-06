package edu.harvard.iq.dataverse.confirmemail;

import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ConfirmEmailUtilTest {

    @ParameterizedTest
    @CsvSource({  
        "48 hours, 2880", 
        "24 hours, 1440",
        "2.75 hours, 165",
        "2.5 hours, 150",
        "1.5 hours, 90", 
        "1 hour, 60", 
        "30 minutes, 30", 
        "1 minute, 1" 
    })
    public void friendlyExpirationTimeTest(String timeAsFriendlyString, int timeInMinutes) {
        assertEquals(timeAsFriendlyString, ConfirmEmailUtil.friendlyExpirationTime(timeInMinutes));
    }

    @Test
    public void testGrandfatheredTime() {
        System.out.println();
        System.out.println("Grandfathered account timestamp test");
        System.out.println("Grandfathered Time (y2k): " + ConfirmEmailUtil.getGrandfatheredTime());
        assertEquals(Timestamp.valueOf("2000-01-01 00:00:00.0"), ConfirmEmailUtil.getGrandfatheredTime());
        System.out.println();
    }
}
