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
         public void testPhoneNoDuplicatedPrefix() throws Exception {
        // given
        String p = "+375 (17) 292 42 84";


        // when
        String res = Place.cleanUpPhone(p);

        // then
        assertEquals("+375 17 292 42 84", res);
    }

    @Test
    public void testPhoneNoDuplicatedPrefix2() throws Exception {
        // given
        String p = "+375(17) 218 84 31";

        // when
        String res = Place.cleanUpPhone(p);

        // then
        assertEquals("+375 17 218 84 31", res);
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
    public void updatePhoneNoDuplicatesAddedEvenNoSpaces() throws Exception {
        String dupPhone = "+375 17 111 11 11";
        String phone = "+375 29 1234567, " + dupPhone;

        Place stub = new Place(-1, -1, null, null, null, null, null, null, phone);

        stub.updatePhoneWith(dupPhone.replaceAll(" ", "").replace("+", ""));

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
    public void workDaysWithDashes() throws Exception {
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

    @Test
    public void phoneRemovesLetters() {
        // given
        String inPhone = "+375 17 306 20 40, Единый номер Call центр 1234";

        // when
        String res = Place.cleanUpPhone(inPhone);

        // then
        assertEquals("+375 17 306 20 40, 1234", res);
    }

    @Test
    public void phoneDoesNotRemoveSemicolon() {
        // given
        String inPhone = "+375 17 306 20 40; 1234";

        // when
        String res = Place.cleanUpPhone(inPhone);

        // then
        assertEquals("+375 17 306 20 40, 1234", res);
    }

    @Test
    public void phoneRemovesEmptyBreckets() {
        // given
        String inPhone = "+375 17 306 20 40, () 1234 ( )";

        // when
        String res = Place.cleanUpPhone(inPhone);

        // then
        assertEquals("+375 17 306 20 40, 1234", res);
    }

    @Test
    public void phoneRemovesEmptyCommaBreckets() {
        // given
        String inPhone = "+375 165 32 33 50, 205 (, ), ), +375 17 209 29 44";

        // when
        String res = Place.cleanUpPhone(inPhone);

        // then
        assertEquals("+375 165 32 33 50, 205, +375 17 209 29 44", res);
    }

    @Test
    public void removeDuplicates() {
        // given
        String inPhone = "7555, +375 17 306 33 14, +375 17 306 33 15, +375 17306 33 14, +375 17 3063315, 7555";
        //"+375 29 309 7 309, +375 33 309 7 309, +375 17 309 7 309, +375 33 309 7 309";

        // when
        //String res = Place.removeDupes(inPhone);

        // then
        //assertEquals("7555, +375 17 306 33 14, +375 17 306 33 15", res);
    }
}