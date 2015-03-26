package ch.prokopovi.auxiliary;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlaceTest {
    String[] WEEK_DAYS = new String[]{"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

    @Test
    public void testWorkWeekDaysShortened() throws Exception {

        for (String day : WEEK_DAYS) {
            String dot = ".";
            String shortened = Place.shortenWorkHours("prefix " + day + dot + " suffix");

            assertTrue(
                    "invalid shortening of day: " + day + " " + shortened,
                    shortened.contains(day) && !shortened.contains(dot));

            String shortenedWithCase = Place.shortenWorkHours("prefix " + day.toLowerCase() + dot + " suffix");
            assertTrue(
                    "invalid shortening of day: " + day,
                    shortenedWithCase.contains(day) && !shortenedWithCase.contains(dot));
        }
    }

    @Test
    public void testPhoneLineBreaksShortened() throws Exception {

        String[] brs = new String[]{"<br>", "<br />", "<br/>", "&lt;br&gt;", "&lt;br /&gt;", "&lt;/br&gt;"};

        for (String br : brs) {
            String shortened = Place.cleanUpPhone("+375 29 123456 " + br);

            assertTrue("line break [" + br + "] was not removed", !shortened.contains(br));
        }
    }

    @Test
    public void testPhonePlusSpaceRemoved() throws Exception {
        String phoneWithPlusAndSpace = "+ 375 ";

        String res = Place.cleanUpPhone(phoneWithPlusAndSpace);

        assertEquals("+375", res);
    }

    @Test
    public void testPhoneDashRemoved() throws Exception {

        String res = Place.cleanUpPhone("+375 29 123-45-67");

        assertTrue("there are dashes in the phone: " + res, !res.contains("-"));
    }

    @Test
    public void testPhoneNoDuplicatesAdded() throws Exception {
        String dupPhone = "+375 17 111 11 11";
        String phone = "+375 29 1234567, " + dupPhone;

        Place stub = new Place(-1, -1, null, null, null, null, null, null, phone);

        stub.updatePhoneWith(dupPhone);

        assertEquals(phone, stub.getPhone());
    }

    @Test
    public void testPlaceWithNullParams() throws Exception {

        Place stub = new Place(-1, -1, null, null, null, null, null, null, null);
    }

    @Test
    public void testCleanUpDuplicatedDotsAndCommas() throws Exception {
        String clean = Place.commonCleanUp("bla.. +375 ,,");

        assertEquals("bla. +375 ,", clean);
    }

    @Test
    public void testWorkDaysWithDashes() throws Exception {
        CharSequence dash = "-";

        for (String day : WEEK_DAYS) {
            String prefix = "prefix";
            String suffix = "suffix";

            String shortenedDashRight = Place.shortenWorkHours(prefix + day.toLowerCase() + dash + suffix);

            assertTrue(shortenedDashRight, shortenedDashRight.contains(prefix));
            assertTrue(shortenedDashRight, shortenedDashRight.contains(day));
            assertTrue(shortenedDashRight, shortenedDashRight.contains(dash));
            assertTrue(shortenedDashRight, shortenedDashRight.contains(suffix));


            String shortenedDashLeft = Place.shortenWorkHours(prefix + dash + day.toLowerCase() + suffix);

            assertTrue(shortenedDashLeft, shortenedDashLeft.contains(prefix));
            assertTrue(shortenedDashLeft, shortenedDashLeft.contains(day));
            assertTrue(shortenedDashLeft, shortenedDashLeft.contains(dash));
            assertTrue(shortenedDashLeft, shortenedDashLeft.contains(suffix));
        }

    }
}