/**
 * $Id: $
 *
 * Copyright (C) 2015 BaseN.
 *
 * All rights reserved.
 */
package net.basen.timecontroller;

/**
 * @author lcu
 *
 */
public enum TimeUnits {
    //U_MSEC(1, "ms", "millisecond", "milliseconds"),
    U_SEC(1000, "s", "second", "seconds"),
    U_MIN(60*1000, "m", "minute", "minutes"),
    U_HOUR(60*60*1000, "h", "hour", "hours"),
    U_WDAY(15*30*60*1000, "wd", "workday", "workdays"),
    U_DAY(24*60*60*1000, "d", "day", "days"),
    ;
    
    long    val;
    String  abbrevName, 
            longName, 
            longNamePlural;
    public static final TimeUnits[] arrayUnits = new TimeUnits[] {
        /* U_MSEC,*/ U_SEC, U_MIN, U_HOUR, U_WDAY,
    };
    private static final long eps = arrayUnits[0].val / 2;
    
    TimeUnits(long val, String abbrevName, String longName, String longNamePlural) {
        this.val            = val; 
        this.abbrevName     = abbrevName; 
        this.longName       = longName; 
        this.longNamePlural = longNamePlural;
    }
    
    public static String msecToString(long value) {
        StringBuffer res = new StringBuffer();
        String sep = "";
        if (value < eps) return "0" + arrayUnits[0].abbrevName;
        value += eps;
        for (int i = arrayUnits.length-1; i >= 0; i--) {
            long integral = value / arrayUnits[i].val;
                    value = value % arrayUnits[i].val;
            if (integral != 0) {
                res.append( sep + integral + arrayUnits[i].abbrevName );
                sep = " ";
            }
        }
        return res.toString();
    }

    /**
     * @return the abbrevName
     */
    public String getAbbrevName() {
        return abbrevName;
    }

    /**
     * @return the longName
     */
    public String getLongName() {
        return longName;
    }

    /**
     * @return the longNamePlural
     */
    public String getLongNamePlural() {
        return longNamePlural;
    }

    /**
     * @return the arrayunits
     */
    public static TimeUnits[] getArrayunits() {
        return arrayUnits;
    }

}
