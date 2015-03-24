package ch.prokopovi.auxiliary;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class PlaceTest {
  @Test
  public void testWorkWeekDaysShortened() throws Exception {

    String[] days = new String[] {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

    for (String day : days) {
      String dot = ".";
      String shortened = Place.shortenWorkHours("prefix "+day+dot+" suffix");

      Assert.assertTrue("invalid shortening of day: "+day+" "+shortened, shortened.contains(day) && !shortened.contains(dot));

      String shortenedWithCase = Place.shortenWorkHours("prefix "+day.toLowerCase()+dot+" suffix");
      Assert.assertTrue("invalid shortening of day: "+day, shortenedWithCase.contains(day) && !shortenedWithCase.contains(dot));
    }
  }

  @Test
  public void testPhoneLineBreaksShortened() throws Exception {

    String[] brs = new String[] {"<br>", "<br />", "<br/>", "&lt;br&gt;", "&lt;br /&gt;"};

    for (String br : brs) {
      String shortened = Place.cleanUpPhone("+375 29 123456 " + br);

      Assert.assertTrue("line break ["+br+"] was not removed", !shortened.contains(br));
    }
  }
}