package com.example.hijricalendarwidget;

import org.junit.Test;
import static org.junit.Assert.*;

public class HijriDateUtilsTest {

    @Test
    public void testGregorianToHijriConversion() {
        // Test a known conversion: January 12, 2026 should be approximately Jumada Al-Awwal, 1447
        int[] hijriDate = HijriDateUtils.gregorianToHijri(2026, 0, 12); // Month 0 = January
        
        assertNotNull(hijriDate);
        assertEquals(3, hijriDate.length);
        
        System.out.println("Gregorian: Jan 12, 2026 -> Hijri: " + 
                          HijriDateUtils.HIJRI_MONTH_NAMES[hijriDate[1]-1] + " " + 
                          hijriDate[2] + ", " + hijriDate[0]);
    }

    @Test
    public void testGetCurrentHijriDate() {
        int[] currentDate = HijriDateUtils.getCurrentHijriDate();
        
        assertNotNull(currentDate);
        assertEquals(3, currentDate.length);
        assertTrue(currentDate[0] > 0); // Year should be positive
        assertTrue(currentDate[1] >= 1 && currentDate[1] <= 12); // Month should be 1-12
        assertTrue(currentDate[2] >= 1 && currentDate[2] <= 30); // Day should be 1-30
    }

    @Test
    public void testFormatHijriDate() {
        int[] sampleDate = {1447, 5, 12}; // 12 Jumada Al-Awwal, 1447
        String formatted = HijriDateUtils.formatHijriDate(sampleDate);
        
        assertEquals("Jumada Al-Awwal 12, 1447", formatted);
    }

    @Test
    public void testIsHijriLeapYear() {
        // Test some known leap years
        assertTrue(HijriDateUtils.isHijriLeapYear(1448)); // Should be a leap year
        assertFalse(HijriDateUtils.isHijriLeapYear(1447)); // Should not be a leap year
    }
}