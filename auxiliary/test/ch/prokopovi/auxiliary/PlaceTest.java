package ch.prokopovi.auxiliary;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlaceTest {
    @Test
    public void testWorkWeekDaysShortened() throws Exception {

        String[] days = new String[]{"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

        for (String day : days) {
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

        String[] brs = new String[]{"<br>", "<br />", "<br/>", "&lt;br&gt;", "&lt;br /&gt;"};

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

}