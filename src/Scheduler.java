import java.util.*;
import java.lang.management.ManagementFactory;

public class Scheduler
{
    private ScheduledTask logger;
    private String time, st, et, times;
    private int lag, activeDays;
    private long margin, scale, interval, offset, single, boot, sVal, eVal;
    private boolean daySelected, start, end;
    private volatile boolean stop;
    private Thread ST_thread, ET_thread, DS_thread, RO_thread, CY_thread, AD_thread, OT_thread, BO_thread;

    public Scheduler (ScheduledTask spec)
    {
        logger = spec;
        lag = 900;
        margin = 1000;
        scale = 0;
        interval = -2;
        offset = -2;
        single = -1;
        boot = -1;
        activeDays = 7;
        times = "IGNORE";
        time = "";
        st = "";
        et = "";
        sVal = -3;
        eVal = -3;
        start = true;
        end = false;
        daySelected = true;
        stop = true;
        AD_thread = new Thread(_advanced);
        CY_thread = new Thread(_cycle);
        RO_thread = new Thread(_routine);
        DS_thread = new Thread(_dayChk);
        ET_thread = new Thread(_eTimer);
        ST_thread = new Thread(_sTimer);
        OT_thread = new Thread(_once);
        BO_thread = new Thread(_startup);
    }

    void changeDelay (int t)
    {
        lag = t;
        margin = t*10;
    }

    public void changeTask (ScheduledTask nl)
    {
        if (nl != null)
            logger = nl;
    }

    public boolean restart()    //Convenience method
    {
        try
        {
            this.terminate();
            this.initiate();
            return true;
        }
        catch (IllegalThreadStateException failed)
        {
            failed.printStackTrace();
            return false;
        }
    }

    public synchronized void initiate() throws IllegalThreadStateException   //Starts this scheduler
    {
        stop = false;

        if (!BO_thread.isAlive() && boot > 0)
            BO_thread.start();
        if (!OT_thread.isAlive() && single > 0)
            OT_thread.start();
        if (!ST_thread.isAlive() && sVal >= 0)
            ST_thread.start();
        if (!ET_thread.isAlive() && eVal >= 0)
            ET_thread.start();
        if (!DS_thread.isAlive() && !(activeDays == 7 || activeDays == 0))
            DS_thread.start();
        
        if (offset < 0 && interval > 999 && !RO_thread.isAlive())
            RO_thread.start();

        else if (offset >= 0 && interval < 999 && !CY_thread.isAlive())
            CY_thread.start();

        else if (offset >= 0 && interval > 999 && !AD_thread.isAlive())
            AD_thread.start();
        
    }
    
    public synchronized void terminate() throws IllegalThreadStateException   //Commands threads to finish executing
    {
        stop = true;

        if (BO_thread.isAlive())
            BO_thread.interrupt();
        if (OT_thread.isAlive())
            OT_thread.interrupt();
        if (ST_thread.isAlive())
            ST_thread.interrupt();
        if (ET_thread.isAlive())
            ET_thread.interrupt();
        if (DS_thread.isAlive())
            DS_thread.interrupt();
        if (RO_thread.isAlive())
            RO_thread.interrupt();
        if (CY_thread.isAlive())
            CY_thread.interrupt();
        if (AD_thread.isAlive())
            AD_thread.interrupt();

        AD_thread = new Thread(_advanced);
        CY_thread = new Thread(_cycle);
        RO_thread = new Thread(_routine);
        DS_thread = new Thread(_dayChk);
        ET_thread = new Thread(_eTimer);
        ST_thread = new Thread(_sTimer);
        OT_thread = new Thread(_once);
        BO_thread = new Thread(_startup);

    }

    public boolean isSet (SchedulerThreads th)
    {
        switch (th) {
            case INTERVAL: return RO_thread.isAlive();
            case OFFSET: return CY_thread.isAlive();
            case ADVANCED: return AD_thread.isAlive();
            case ONCE: return OT_thread.isAlive();
            case AFTER_STARTUP: return BO_thread.isAlive();
            case START_TIME: return ST_thread.isAlive();
            case END_TIME: return ET_thread.isAlive();
            case DAYS_OF_WEEK: return DS_thread.isAlive();
            default: return false;
        }
    }

    private long snooze (Date future)   //Used for setting time in Thread.sleep (long millis)
    {
        long target = future.getTime();
        long now = System.currentTimeMillis();
        long dist = target - now;
        if (dist < margin)
            return 0;
        else
            return dist - margin;
    }
    
    private long timeFrame (String size, boolean scalar)
    {
        Calendar base = Calendar.getInstance();
        base.setTime (new Date());
        long addon = 0;
        
        switch (size.length()) 
        {
            case 0: return -1;
            case 1:     //Second
            {
                base.set (14,0);
                base.set (13,0);
                addon = 60000;
                break;
            }
            case 2:     //Second
            {
                base.set (14,0);
                base.set (13,0);
                addon = 60000;
                break;
            }
            case 4:     //Minute
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                addon = 3600000;
                break;
            }
            case 5:     //Minute
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                addon = 3600000;
                break;
            }
            case 7:     //Hour
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                addon = 86400000;
                break;
            }
            case 8:     //Hour
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                addon = 86400000;
                break;
            }
            case 10:    //Day of the month
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                base.set (5,0);
                addon = (86400000*getDaysInMonth (base.get(2), base.get(1)));
                break;
            }
            case 11:    //Day of the month
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                base.set (5,0);
                addon = (86400000*getDaysInMonth (base.get(2), base.get(1)));
                break;
            }
            case 12:    //Day of the week
            {
                int dom = base.get(5)-(base.get(7)-1);
                base.set (5, dom);
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                addon = 86400000*activeDays;
                break;
            }
            case 15:    //Month of the year (day of month)
            {
                int mon = sToMonth (size.substring (0,3))-1;
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                base.set (5,0);
                base.set (2,mon);
                addon = 86400000*getDayOfYear();
                break;
            }
            case 16:    //Month of the year (weekday)
            {
                int mon = sToMonth (size.substring (0,3))-1;
                int dom = base.get(5)-(base.get(7)-1);
                base.set (2,mon);
                base.set (5, dom);
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                addon = 86400000*getDayOfYear();
                break;
            }
        }
        if (scalar)
            return addon;
        else
            return base.getTime().getTime();
    }
    
    static long convert (String syntax, boolean care)  //Transform a time (e.g. "12:35:00") into milliseconds
    {
        long relative = 1000;
        int size = syntax.length();
        try
        {
            if (size == 0)
                return -1;
            
            else if (size == 12 || (size > 14 && size < 20))
                return sToDate(syntax).getTime();
            
            else if (size == 2 || size == 1)   //Seconds
            {
                int seconds = Integer.parseInt (syntax);
                
                if (care && seconds > 59)
                    return -2;
                else
                    relative *= seconds;
            }
            
            else if (size == 4)   //Minutes
            {
                int minutes = Integer.parseInt (syntax.substring (0,1));
                int seconds = Integer.parseInt (syntax.substring (2));
                
                if (care && (minutes > 59 || seconds > 59))
                    return -2;
                else
                    relative *= (minutes*60)+seconds;
            }

            else if (size == 5)   //Minutes
            {
                int minutes = Integer.parseInt (syntax.substring (0,2));
                int seconds = Integer.parseInt (syntax.substring (3));
                
                if (care && (minutes > 59 || seconds > 59))
                    return -2;
                else
                    relative *= (minutes*60)+seconds;
            }
            
            else if (size == 7)   //Hours
            {
                int hours = Integer.parseInt (syntax.substring (0,1));
                int minutes = Integer.parseInt (syntax.substring (2,4));
                int seconds = Integer.parseInt (syntax.substring (5));
                
                if (care && (hours > 23 || minutes > 59 || seconds > 59))
                    return -2;
                else
                    relative *= (hours*3600)+(minutes*60)+seconds;
            }

            else if (size == 8)   //Hours
            {
                int hours = Integer.parseInt (syntax.substring (0,2));
                int minutes = Integer.parseInt (syntax.substring (3,5));
                int seconds = Integer.parseInt (syntax.substring (6));
                
                if (care && (hours > 23 || minutes > 59 || seconds > 59))
                    return -2;
                else
                    relative *= (hours*3600)+(minutes*60)+seconds;
            }
            
            else if (size == 10)  //Days
            {
                int days = Integer.parseInt (syntax.substring (0,1));
                int hours = Integer.parseInt (syntax.substring (2,4));
                int minutes = Integer.parseInt (syntax.substring (5,7));
                int seconds = Integer.parseInt (syntax.substring (8));
                
                if (care && (days > 31 || hours > 23 || minutes > 59 || seconds > 59))
                    return -2;
                else
                    relative *= (days*86400)+(hours*3600)+(minutes*60)+seconds;
            }

            else if (size == 11)  //Days
            {
                int days = Integer.parseInt (syntax.substring (0,2));
                int hours = Integer.parseInt (syntax.substring (3,5));
                int minutes = Integer.parseInt (syntax.substring (6,8));
                int seconds = Integer.parseInt (syntax.substring (9));
                
                if (care && (days > 31 || hours > 23 || minutes > 59 || seconds > 59))
                    return -2;
                else
                    relative *= (days*86400)+(hours*3600)+(minutes*60)+seconds;
            }
            
            else if (size == 13)   //Months
            {
                int months = Integer.parseInt (syntax.substring (0,1));
                int days = Integer.parseInt (syntax.substring (2,4));
                int hours = Integer.parseInt (syntax.substring (5,7));
                int minutes = Integer.parseInt (syntax.substring (8,10));
                int seconds = Integer.parseInt (syntax.substring (11));
                
                if (care && (months > 12 || days > 31 || hours > 23 || minutes > 59 || seconds > 59))
                    return -2;
                else
                    relative *= (months*2592000)+(days*86400)+(hours*3600)+(minutes*60)+seconds;
            }
            
            else if (size == 14)   //Months
            {
                Integer.parseInt (syntax.substring (0,2));
                int days = Integer.parseInt (syntax.substring (3,5));
                int hours = Integer.parseInt (syntax.substring (6,8));
                int minutes = Integer.parseInt (syntax.substring (9,11));
                int seconds = Integer.parseInt (syntax.substring (12));
                
                if (care && (days > 31 || hours > 23 || minutes > 59 || seconds > 59))
                    return -2;
                else
                    relative *= (days*86400)+(hours*3600)+(minutes*60)+seconds;
            }
            
            else
                return -2;
        }
            
        catch (NumberFormatException NaN)
        {
            System.err.println ("Incorrect syntax entered.");
            return -3;
        }
        catch (NullPointerException nv)
        {
            System.err.println ("No value entered.");
            return -4;
        }

        return relative;
    }

    public static boolean isValidTime (String syntax)
    {
        return convert(syntax,true) > -2;
    }

    public static boolean isValidDay (String syntax)
    {
        return new Scheduler(null).setDays(syntax).length() > 2;
    }

    private static Date sToDate (String syntax)
    {
        Calendar at = Calendar.getInstance();
        String day, month, year;
        String second = "0", minute = "0", hour = "0";
        Calendar curr = Calendar.getInstance();
        curr.setTime (new Date());
        
        if (syntax.length() > 15)
        {
            try     //yyyy-mo-ddT24:mi
            {
                Integer.parseInt (syntax.substring (0,4));
                year = syntax.substring (0,4);
                month = syntax.substring (5,7);
                day = syntax.substring (8,10);
            }
            catch (NumberFormatException alt)   //dd:mo:yyyyT24:mi
            {
                day = syntax.substring (0,2);
                month = syntax.substring (3,5);
                year = syntax.substring (6,10);
            }
            hour = syntax.substring (11,13);
            minute = syntax.substring (14,16);
            if (syntax.length() > 16)
                    second = syntax.substring (17);
        }
        
        else if (syntax.length() == 15)     
        {
            year = ""+curr.get(1);
            hour = syntax.substring (7,9);
            minute = syntax.substring (10,12);
            second = syntax.substring (13);
            try     //dd-mon_hh:mi:ss
            {
                day = ""+Integer.parseInt (syntax.substring(0,2));
                int m = sToMonth (syntax.substring(3,6));
                month = ""+m;
            }
            catch (NumberFormatException alt)   //mon-dd_hh:mi:ss
            {
                month = ""+sToMonth (syntax.substring(0,3));
                day = syntax.substring (4,6);
            }
        }
        
        else if (syntax.length() == 12)     //dow-hh:mi:ss
        {
            year = ""+curr.get(1);
            month = ""+(curr.get(2)+1);
            int diff = compareDays (syntax.substring(0,3), curr.get(7));
            day = ""+(curr.get(5)+diff);
            hour = syntax.substring (4,6);
            minute = syntax.substring (7,9);
            second = syntax.substring (10);
        }
        
         else
        {
            day = ""+curr.get(5)+1;
            month = ""+(curr.get(2)+1); 
            year = ""+curr.get(1);
        }
        
        /*if (syntax.length() == 12 || syntax.length() == 15)
        {
            try
            {
                if (curr.get(1) == Integer.parseInt(year))
                {
                    if (curr.get(2) > Integer.parseInt(month))
                        year = ""+(curr.get(1)+1);

                    else if (curr.get(5) > Integer.parseInt(day) && curr.get(2) == Integer.parseInt(month))
                        year = ""+(curr.get(1)+1);

                    else if (curr.get(11) > Integer.parseInt(hour) && curr.get(5) == Integer.parseInt(day))
                        year = ""+(curr.get(1)+1);

                    else if (curr.get(12) > Integer.parseInt(minute) && curr.get(11) == Integer.parseInt(hour))
                        year = ""+(curr.get(1)+1);

                    else if (curr.get(13) > Integer.parseInt(second) && curr.get(12) == Integer.parseInt(minute))
                        year = ""+(curr.get(1)+1);
                }
            }
            catch (NumberFormatException NaN)
            {
                System.out.println ("Incorrect date syntax entered.");
            }
        }*/
        
        try
        {
            if (Integer.parseInt (month) > 12 && Integer.parseInt (day) < 13)
            {
                String tmp = day;
                day = month;
                month = tmp;
            }
            
            at.set (5, Integer.parseInt (day));
            at.set (2, Integer.parseInt (month)-1);
            at.set (1, Integer.parseInt (year));
            at.set (11, Integer.parseInt (hour));
            at.set (12, Integer.parseInt (minute));
            at.set (13, Integer.parseInt (second));
            at.set (14, 0);
        }
        catch (NumberFormatException NaN)
        {
            System.out.println ("Incorrect date syntax entered.");
        }

        return at.getTime();
    }

    public enum SchedulerThreads {
        DAYS_OF_WEEK, START_TIME, END_TIME, ONCE, AFTER_STARTUP, INTERVAL, OFFSET, ADVANCED
    }

    public enum Day {
        Saturday, Sunday, Monday, Tuesday, Wednesday, Thursday, Friday
    }

    public enum Month {
        January, February, March, April, May, June, July, August, September, November, December
    }

    static int sToWeek (String dow)
    {
        dow = dow.toUpperCase();
        switch (dow.substring(0,3)) {
            case "SUN": return 1;
            case "MON": return 2;
            case "TUE": return 3;
            case "WED": return 4;
            case "THU": return 5;
            case "FRI": return 6;
            case "SAT": return 7;
            default: switch (dow) {
                case "WEEKDAY": return 8;
                case "WEEKEND": return 9;
                default: return 0;
            }
        }
    }
    
    static String weekToS (int dow)
    {
        switch (dow)
        {
            case 1: return "SUN";
            case 2: return "MON";
            case 3: return "TUE";
            case 4: return "WED";
            case 5: return "THU";
            case 6: return "FRI";
            case 7: return "SAT";
            case 8: return "WEEKDAY";
            case 9: return "WEEKEND";
            default: return "unknown";
        }
    }
    
    private static int compareDays (String dow, int other)
    {
        return other > 7 ? -8 : other-sToWeek(dow);
    }
    
    static int sToMonth (String month)
    {
        switch (month.toUpperCase().substring(0,3))
        {
            case "JAN": return 1;
            case "FEB": return 2;
            case "MAR": return 3;
            case "APR": return 4;
            case "MAY": return 5;
            case "JUN": return 6;
            case "JUL": return 7;
            case "AUG": return 8;
            case "SEP": return 9;
            case "OCT": return 10;
            case "NOV": return 11;
            case "DEC": return 12;
            case "UND": return 13;

        }
        try
        {
            return Integer.parseInt (month.substring(0,2));
        }
        catch (NumberFormatException NaN)
        {
            System.err.println ("Incorrect syntax entered for MONTH.");
            return 0;
        }
    }
    
    static int getDaysInMonth (int month, int year)
    {
        int dim = 31;

        if (month == 3 || month == 5 || month == 8 || month == 10)
            dim = 30;
        else if (month == 1)
        {
            boolean leapYear;
            // divisible by 4
            leapYear = (year % 4 == 0);
            // divisible by 4 and not 100
            leapYear = leapYear && (year % 100 != 0);
            // divisible by 4 and not 100 unless divisible by 400
            leapYear = leapYear || (year % 400 == 0);
            
            dim = (leapYear) ? 29 : 28;
        }
        return dim;
    }
    
    static int getDayOfYear()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime (new Date());
        /*int days = cal.get(5);
        if (cal.get(2) > 0)
            for (int i = 0; i < cal.get(2); i++)
                days += getDaysInMonth (i, cal.get(1));
        return days;*/
        return cal.get(Calendar.DAY_OF_YEAR);
    }
    
    long getFrequency()      //Calculate the effective equivalent of interval logging
    {
        try
        {
            if (interval > 999 && offset >= 0)  
            {
                int num = (int)(scale/interval);

                if (activeDays < 7 && activeDays > 0 && scale <= 86400000)
                {
                    Double mult = (double) (7/activeDays);
                    Double dVal = Math.floor((scale/num)*mult.longValue());
                    return dVal.longValue();
                }

                else
                    return (long) Math.floor (scale/num);
            }

            else if (interval < 999 && offset >= 0)   
            {
                if (activeDays < 7 && activeDays > 0 && scale <= 86400000)
                {
                    Double mult = (double) (7/activeDays);
                    return (long) Math.floor (scale*mult.longValue());
                }
                else
                    return scale;

            }

            else if (interval > 999 && offset < 0)  
            {
                if (activeDays < 7 && activeDays > 0)
                {
                    Double mult = (double) (7/activeDays);
                    return (long) Math.floor (interval*mult.longValue());
                }
                else
                    return interval;
            }
            
            else
                return -1;
        }
        catch (ArithmeticException ae)
        {
            return -2;
        }
    }

    //How long until points are depleted
    public static String Capacity (long frequency, int points, int total)
    {
        int times;
        String lim;
        times = points/total;

        long Val = frequency * times;

        if (Val > 1000)
        {
            int seconds = (int)Math.ceil (Val/1000);

            if (seconds > 60)
            {
                int minutes = (int)Math.ceil (seconds/60);
                seconds -= (minutes*60);

                if (minutes > 60)
                {
                    int hours = (int)Math.ceil (minutes/60);
                    minutes -= (hours*60);

                    if (hours > 24)
                    {
                        int days = (int)Math.ceil (hours/24);
                        hours -= (days*24);

                        if (days > 7)
                        {
                            int weeks = (int)Math.ceil (days/7);
                            days -= (weeks*7);
                            lim = weeks+" weeks, "+days+" days, "+hours+" hours, "+minutes+" minutes, "+seconds+" seconds";
                        }
                        else
                            lim = days+" days, "+hours+" hours, "+minutes+" minutes, "+seconds+" seconds";
                    }
                    else
                        lim = hours+" hours, "+minutes+" minutes, "+seconds+" seconds";
                }
                else
                    lim = minutes+" minutes, "+seconds+" seconds";
            }
            else
                lim = seconds+" seconds";
        }
        else
            lim = "None! Please revise your logging frequency.";

        return lim;
    }

    //How long until points are depleted for these settings
    public String Capacity (int points, int total)
    {
        return Capacity(getFrequency(), points, total);
    }

    public boolean setInterval (String inter)
    {
        if (inter.length() < 20)
        {
            interval = convert(inter, false);
            if (interval == 0)
                return false;
        }
        else
            interval = -2;
        
        return (interval > -2); //Whether a time could be successfully parsed
    }
    
    public boolean setOffset (String off)
    {
        time = off;
        reset(new Date());
        if (off.length() > 1)
        {
            if (off.length() < 13)
                offset = convert(off, true);
            else    //Ignore the month and year
                offset = convert(off.substring(off.length()-13), true);
        }
        else
            offset = -1;
        
        return (offset > -2);   //Whether a time could be successfully parsed
    }
    
    public boolean setStart (String stime)
    {
        st = stime;
        sVal = convert(st,true);
        return (sVal > -2);     //Whether a time could be successfully parsed
    }
    
    public boolean setEnd (String etime)
    {
        et = etime;
        eVal = convert(et,true);
        return (eVal > -2);    //Whether a time could be successfully parsed
    }
    
    public String setDays (String w)    //Days of week selected
    {
        if (w == null)
            return "";
        else if (w.equalsIgnoreCase("NO") || w.equalsIgnoreCase ("None") || w.equalsIgnoreCase ("void") || w.equalsIgnoreCase("IGNORE"))
        {
            times = "NONE";
            activeDays = 7;
            return w;
        }
        else if (w.equalsIgnoreCase ("Every day") || w.equalsIgnoreCase("ignore") || w.equalsIgnoreCase ("All") || w.equalsIgnoreCase("All days"))
        {
            times = "ALL";
            activeDays = 7;
            return w;
        }
        else if (w.length() < 3)
            return "";
        
        times = w.toUpperCase();
        boolean Mon = times.contains("MON");
        boolean Tue = times.contains("TUE");
        boolean Wed = times.contains("WED");
        boolean Thu = times.contains("THU");
        boolean Fri = times.contains("FRI");
        boolean Sat = times.contains("SAT");
        boolean Sun = times.contains("SUN");
        boolean wkdays = times.contains("WEEKDAY") || (Mon && Tue && Wed && Thu && Fri);
        boolean wkends = times.contains("WEEKEND") || (Sat && Sun);
        
        if (wkdays && wkends)
        {
            times = "ALL";
            activeDays = 7;
        }
        else if (wkdays)
        {
            activeDays = 5;
            times = "Weekdays";
            if (Sat)
            {
                times += " Saturday";
                activeDays++;
            }
            if (Sun)
            {
                times += " Sunday";
                activeDays++;
            }
        }
        else if (wkends)
        {
            activeDays = 2;
            times = "Weekends";
            if (Mon)
            {
                times += " Monday";
                activeDays++;
            }
            if (Tue)
            {
                times += " Tuesday";
                activeDays++;
            }
            if (Wed)
            {
                times += " Wednesday";
                activeDays++;
            }
            if (Thu)
            {
                times += " Thursday";
                activeDays++;
            }
            if (Fri)
            {
                times += " Friday";
                activeDays++;
            }
        }
        else    //Neither weekends nor weekdays
        {
            activeDays = 0;
            times = "";
            if (Mon)
            {
                times += " Monday";
                activeDays++;
            }
            if (Tue)
            {
                times += " Tuesday";
                activeDays++;
            }
            if (Wed)
            {
                times += " Wednesday";
                activeDays++;
            }
            if (Thu)
            {
                times += " Thursday";
                activeDays++;
            }
            if (Fri)
            {
                times += " Friday";
                activeDays++;
            }
            if (Sat)
            {
                times += " Saturday";
                activeDays++;
            }
            if (Sun)
            {
                times += " Sunday";
                activeDays++;
            }
        }
        return times;
    }
    
    public boolean setOnce(String moment)
    {
        single = convert(moment, true);

        if (single >= 0 && single < 365*24*3600*1000L)
            single += timeFrame (moment,false);     //If not a date, find the next occurence of the specified time

        return (single > -2);   //Whether a time could be successfully parsed
    }
    
    public boolean setAfterStartup(String elapse)
    {
        boot = convert(elapse, false);
        return boot != 0 && (boot > -2);
    }

    private long reset (Date from)
    {
        Calendar base = Calendar.getInstance();
        base.setTime (from);

        switch (time.length())  //Example: if time="13:37:00", set the time to the current date but at midnight (00:00:00)
        {
            case 0:
            {
                scale = 0;
                return 0;
            }
            case 1:     //Second
            {
                base.set (14,0);
                base.set (13,0);
                scale = 60000;
                break;
            }
            case 2:     //Second
            {
                base.set (14,0);
                base.set (13,0);
                scale = 60000;
                break;
            }
            case 4:     //Minute
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                scale = 3600000;
                break;
            }
            case 5:     //Minute
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                scale = 3600000;
                break;
            }
            case 7:     //Hour
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                scale = 86400000;
                break;
            }
            case 8:     //Hour
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                scale = 86400000;
                break;
            }
            case 10:    //Day of the month
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                base.set (5,0);
                scale = (86400000*getDaysInMonth (base.get(2), base.get(1)));
                break;
            }
            case 11:    //Day of the month
            {
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                base.set (5,0);
                scale = (86400000*getDaysInMonth (base.get(2), base.get(1)));
                break;
            }
            case 12:    //Day of the week
            {
                int dom = base.get(5)-(base.get(7)-1);
                base.set (5, dom);
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                scale = 86400000*activeDays;
                break;
            }
            case 15:    //Month of the year (day of month)
            {
                int mon = sToMonth (time.substring (0,3))-1;
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                base.set (5,0);
                base.set (2,mon);
                scale = 86400000*getDayOfYear();
                break;
            }
            case 16:    //Month of the year (weekday)
            {
                int mon = sToMonth (time.substring (0,3))-1;
                int dom = base.get(5)-(base.get(7)-1);
                base.set (2,mon);
                base.set (5, dom);
                base.set (14,0);
                base.set (13,0);
                base.set (12,0);
                base.set (11,0);
                scale = 86400000*getDayOfYear();
                break;
            }
        }
        return base.getTime().getTime();
    }
    
    private final Runnable _sTimer = new Runnable()    //Checks if the start time has passed
    {
        void nextOccurrence()
        {
            start = false;
            long sTarg = sVal + timeFrame(st, false);
            try {
                Thread.sleep (snooze (new Date(sTarg)));
            }
            catch (InterruptedException ie) {
                //exit
            }
            while (!stop)
            {
                if (System.currentTimeMillis() >= sTarg)
                {
                    start = true;
                    break;
                }
            }
        }

        public void run()
        {
            final long seventyone = 365*24*3600*1000L;   //Values greater than this are considered a date rather than a recurring time
            if (sVal > System.currentTimeMillis())   //Wait until the target time
            {
                start = false;
                try {
                    Thread.sleep (snooze (new Date(sVal)));
                }
                catch (InterruptedException ie) {
                    //exit
                }
                while (!stop)
                    if (System.currentTimeMillis() >= sVal)
                    {
                        start = true;
                        break;
                    }
            }

            else if (sVal < seventyone && sVal >= 0)  //Regular check
            {
                if (st.length() > 11 && st.length() < 15 || st.length() == 16)
                    nextOccurrence();

                else
                {
                    Calendar current = Calendar.getInstance();
                    check: while (!stop)
                    {
                        int init = 0;
                        current.setTime(new Date());

                        if (st.length() == 15)
                        {
                            int mo = sToMonth(st.substring(0, 3));
                            if (mo > current.get(2))
                            {
                                start = true;
                                break;
                            }
                            init = 3;
                        }

                        switch (st.length()-init)
                        {
                            case 2:
                            {
                                int s;
                                try
                                {
                                    s = Integer.parseInt(st.substring(init, init+2));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for START TIME.");
                                    start = true;
                                    break check;
                                }
                                start = current.get(13) >= s;
                                break;
                            }

                            case 4:
                            {
                                int s, m;
                                try
                                {
                                    m = Integer.parseInt(st.substring(init, init+1));
                                    s = Integer.parseInt(st.substring(init+2, init+4));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for START TIME.");
                                    start = true;
                                    break check;
                                }

                                start = current.get(12) > m || current.get(12) == m && current.get(13) >= s;
                                break;
                            }
                            case 5:
                            {
                                int s, m;
                                try
                                {
                                    m = Integer.parseInt(st.substring(init, init+2));
                                    s = Integer.parseInt(st.substring(init+3, init+5));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for START TIME.");
                                    start = true;
                                    break check;
                                }
                                start = current.get(12) > m || current.get(12) == m && current.get(13) >= s;
                                break;
                            }

                            case 7:
                            {
                                int s, m, h;
                                try
                                {
                                    h = Integer.parseInt(st.substring(init, init+1));
                                    m = Integer.parseInt(st.substring(init+2, init+4));
                                    s = Integer.parseInt(st.substring(init+5, init+7));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for START TIME.");
                                    start = true;
                                    break check;
                                }

                                if (current.get(11) > h)    //Past the target hour
                                    start = true;

                                else if (current.get(11) == h)  //Same hour...
                                {
                                    if (current.get(12) > m)    //...past the target minute
                                        start = true;
                                    else if (current.get(12) == m && current.get(13) >= s)  //...same minute, past the target second
                                        start = true;
                                    else    //Before target time
                                        start = false;
                                } else    //Before target time
                                    start = false;

                                break;
                            }
                            case 8:
                            {
                                int s, m, h;
                                try
                                {
                                    h = Integer.parseInt(st.substring(init, init+2));
                                    m = Integer.parseInt(st.substring(init+3, init+5));
                                    s = Integer.parseInt(st.substring(init+6, init+8));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for START TIME.");
                                    start = true;
                                    break check;
                                }

                                if (current.get(11) > h)    //Past the target hour
                                    start = true;

                                else if (current.get(11) == h)     //Same hour...
                                {
                                    if (current.get(12) > m)    //...past the target minute
                                        start = true;
                                    else if (current.get(12) == m && current.get(13) >= s)    //...same minute, past the target second
                                        start = true;
                                    else    //Before target time
                                        start = false;
                                } else
                                    start = false;

                                break;
                            }

                            case 10:
                            {
                                int s, m, h, d;
                                try
                                {
                                    d = Integer.parseInt(st.substring(init, init+1));
                                    h = Integer.parseInt(st.substring(init+2, init+4));
                                    m = Integer.parseInt(st.substring(init+5, init+7));
                                    s = Integer.parseInt(st.substring(init+8, init+10));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for START TIME.");
                                    start = true;
                                    break check;
                                }

                                if (current.get(5) > d)    //Past the day
                                    start = true;

                                else if (current.get(5) == d)   //Same day...
                                {
                                    if (current.get(11) > h)    //...past the hour
                                        start = true;
                                    else if (current.get(11) == h)  //...same hour...
                                    {
                                        if (current.get(12) > m)    //...past the minute
                                            start = true;
                                        else if (current.get(12) == m && current.get(13) >= s)  //...same minute, past the second
                                            start = true;
                                        else    //Before target time
                                            start = false;
                                    } else    //Before target time
                                        start = false;
                                } else    //Before target time
                                    start = false;

                                break;
                            }
                            case 11:
                            {
                                int s, m, h, d;
                                try
                                {
                                    d = Integer.parseInt(st.substring(init, init+2));
                                    h = Integer.parseInt(st.substring(init+3, init+5));
                                    m = Integer.parseInt(st.substring(init+6, init+8));
                                    s = Integer.parseInt(st.substring(init+9, init+11));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for START TIME.");
                                    start = true;
                                    break check;
                                }

                                if (current.get(5) > d)    //Past the day
                                    start = true;

                                else if (current.get(5) == d)   //Same day...
                                {
                                    if (current.get(11) > h)    //...past the hour
                                        start = true;
                                    else if (current.get(11) == h)  //...same hour...
                                    {
                                        if (current.get(12) > m)    //...past the minute
                                            start = true;
                                        else if (current.get(12) == m && current.get(13) >= s)  //...same minute, past the second
                                            start = true;
                                        else    //Before target time
                                            start = false;
                                    } else    //Before target time
                                        start = false;
                                } else    //Before target time
                                    start = false;

                                break;
                            }

                            default:
                            {
                                nextOccurrence();
                                break check;
                            }
                        }

                        long wake;  //Time to wait until the next time when 'start' is due to change
                        if (start)
                        {
                            long diff;
                            if (sVal > seventyone)
                                diff = System.currentTimeMillis()-sVal;
                            else
                                diff = System.currentTimeMillis()-(timeFrame(st, false)+sVal);

                            wake = timeFrame(st, true)-sVal-diff;
                        } else
                            wake = snooze(new Date(timeFrame(st, false)+sVal));

                        try
                        {
                            Thread.sleep(wake);
                        } catch (InterruptedException f) {
                            break;
                        }
                    }
                }
            }
            else  //Past the target time
                start = System.currentTimeMillis() > sVal && sVal > seventyone || sVal < 0;

        }
    };

    private final Runnable _eTimer = new Runnable()    //Checks if the end time has passed
    {
        void nextOccurrence()
        {
            end = false;
            long eTarg = eVal + timeFrame(et, false);
            try {
                Thread.sleep (snooze (new Date(eTarg)));
            }
            catch (InterruptedException ie) {
                //exit
            }
            while (!stop)
            {
                if (System.currentTimeMillis() >= eTarg)
                {
                    end = true;
                    break;
                }
            }
        }

        public void run()
        {
            final long seventyone = 365*24*3600*1000L;   //Values greater than this are considered a date rather than a recurring time
            if (eVal > System.currentTimeMillis())   //Wait until the target time
            {
                end = false;
                try {
                    Thread.sleep (snooze (new Date(eVal)));
                }
                catch (InterruptedException ie) {
                    //exit
                }
                while (!stop)
                    if (System.currentTimeMillis() >= eVal)
                    {
                        end = true;
                        break;
                    }
            }

            else if (eVal < seventyone && eVal >= 0)  //Regular check
            {
                if (et.length() > 11 && et.length() < 15 || et.length() == 16)
                    nextOccurrence();

                else
                {
                    Calendar current = Calendar.getInstance();
                    check: while (!stop)
                    {
                        int init = 0;
                        current.setTime(new Date());

                        if (et.length() == 15)
                        {
                            int mo = sToMonth(et.substring(0, 3));
                            if (mo > current.get(2))
                            {
                                end = true;
                                break;
                            }
                            init = 3;
                        }

                        switch (et.length()-init)
                        {
                            case 2:
                            {
                                int s;
                                try
                                {
                                    s = Integer.parseInt(et.substring(init, init+2));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for END TIME.");
                                    end = true;
                                    break check;
                                }

                                if (current.get(13) >= s)   //Past the target second
                                    end = true;
                                else    //Before target time
                                    end = false;

                                break;
                            }

                            case 4:
                            {
                                int s, m;
                                try
                                {
                                    m = Integer.parseInt(et.substring(init, init+1));
                                    s = Integer.parseInt(et.substring(init+2, init+4));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for END TIME.");
                                    end = true;
                                    break check;
                                }

                                if (current.get(12) > m)    //Past the target minute
                                    end = true;

                                else if (current.get(12) == m && current.get(13) >= s)  //Same minute, past the target second
                                    end = true;

                                else    //Before target time
                                    end = false;

                                break;
                            }
                            case 5:
                            {
                                int s, m;
                                try
                                {
                                    m = Integer.parseInt(et.substring(init, init+2));
                                    s = Integer.parseInt(et.substring(init+3, init+5));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for END TIME.");
                                    end = true;
                                    break check;
                                }

                                if (current.get(12) > m)    //Past the target minute
                                    end = true;

                                else if (current.get(12) == m && current.get(13) >= s)  //Same minute, past the target second
                                    end = true;

                                else    //Before target time
                                    end = false;

                                break;
                            }

                            case 7:
                            {
                                int s, m, h;
                                try
                                {
                                    h = Integer.parseInt(et.substring(init, init+1));
                                    m = Integer.parseInt(et.substring(init+2, init+4));
                                    s = Integer.parseInt(et.substring(init+5, init+7));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for END TIME.");
                                    end = true;
                                    break check;
                                }

                                if (current.get(11) > h)    //Past the target hour
                                    end = true;

                                else if (current.get(11) == h)  //Same hour...
                                {
                                    if (current.get(12) > m)    //...past the target minute
                                        end = true;
                                    else if (current.get(12) == m && current.get(13) >= s)  //...same minute, past the target second
                                        end = true;
                                    else    //Before target time
                                        end = false;
                                } else    //Before target time
                                    end = false;

                                break;
                            }
                            case 8:
                            {
                                int s, m, h;
                                try
                                {
                                    h = Integer.parseInt(et.substring(init, init+2));
                                    m = Integer.parseInt(et.substring(init+3, init+5));
                                    s = Integer.parseInt(et.substring(init+6, init+8));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for END TIME.");
                                    end = true;
                                    break check;
                                }

                                if (current.get(11) > h)    //Past the target hour
                                    end = true;

                                else if (current.get(11) == h)     //Same hour...
                                {
                                    if (current.get(12) > m)    //...past the target minute
                                        end = true;
                                    else if (current.get(12) == m && current.get(13) >= s)    //...same minute, past the target second
                                        end = true;
                                    else    //Before target time
                                        end = false;
                                } else
                                    end = false;

                                break;
                            }

                            case 10:
                            {
                                int s, m, h, d;
                                try
                                {
                                    d = Integer.parseInt(et.substring(init, init+1));
                                    h = Integer.parseInt(et.substring(init+2, init+4));
                                    m = Integer.parseInt(et.substring(init+5, init+7));
                                    s = Integer.parseInt(et.substring(init+8, init+10));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for END TIME.");
                                    end = true;
                                    break check;
                                }

                                if (current.get(5) > d)    //Past the day
                                    end = true;

                                else if (current.get(5) == d)   //Same day...
                                {
                                    if (current.get(11) > h)    //...past the hour
                                        end = true;
                                    else if (current.get(11) == h)  //...same hour...
                                    {
                                        if (current.get(12) > m)    //...past the minute
                                            end = true;
                                        else if (current.get(12) == m && current.get(13) >= s)  //...same minute, past the second
                                            end = true;
                                        else    //Before target time
                                            end = false;
                                    } else    //Before target time
                                        end = false;
                                } else    //Before target time
                                    end = false;

                                break;
                            }
                            case 11:
                            {
                                int s, m, h, d;
                                try
                                {
                                    d = Integer.parseInt(et.substring(init, init+2));
                                    h = Integer.parseInt(et.substring(init+3, init+5));
                                    m = Integer.parseInt(et.substring(init+6, init+8));
                                    s = Integer.parseInt(et.substring(init+9, init+11));
                                } catch (NumberFormatException NaN)
                                {
                                    System.out.println("Incorrect syntax entered for END TIME.");
                                    end = true;
                                    break check;
                                }

                                if (current.get(5) > d)    //Past the day
                                    end = true;

                                else if (current.get(5) == d)   //Same day...
                                {
                                    if (current.get(11) > h)    //...past the hour
                                        end = true;
                                    else if (current.get(11) == h)  //...same hour...
                                    {
                                        if (current.get(12) > m)    //...past the minute
                                            end = true;
                                        else if (current.get(12) == m && current.get(13) >= s)  //...same minute, past the second
                                            end = true;
                                        else    //Before target time
                                            end = false;
                                    } else    //Before target time
                                        end = false;
                                } else    //Before target time
                                    end = false;

                                break;
                            }

                            default:
                            {
                                nextOccurrence();
                                break check;
                            }
                        }

                        long wake;  //Time to wait until the next time when 'start' is due to change
                        if (end)
                        {
                            long diff;
                            if (eVal > seventyone)
                                diff = System.currentTimeMillis()-eVal;
                            else
                                diff = System.currentTimeMillis()-(timeFrame(et, false)+eVal);

                            wake = timeFrame(et, true)-eVal-diff;
                        } else
                            wake = snooze(new Date(timeFrame(et, false)+eVal));

                        try
                        {
                            Thread.sleep(wake);
                        } catch (InterruptedException f)
                        {
                            break;
                        }
                    }
                }
            }
            else  //Past the target time
                end = System.currentTimeMillis() > eVal && eVal > seventyone || eVal >= 0;
        }
    };
    
    private final Runnable _dayChk = (() ->    //Checks if today is one of the days specified in "times"
    {
        daySelected = false;    //Default assumption
        times = times.toUpperCase();
        while (!stop)
        {
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            int weekday = now.get(7);

            for (int i = 1; i < 8; i++) //Search string for days of week and compare to current day
            {
                if (weekday == i)
                {
                    if (times.contains(weekToS(8)) && (i < 7 && i > 1))  //Weekday
                    {
                        daySelected = true;
                        break;
                    }
                    if (times.contains(weekToS(i)))    //Particular day
                    {
                        daySelected = true;
                        break;
                    }
                    if (times.contains(weekToS(9)) && (i == 7 || i == 1))  //Weekend
                    {
                        daySelected = true;
                        break;
                    }
                    daySelected = false;
                }
            }

            try
            {
                now.setTime(new Date());
                long millis = now.get(14);
                long secs = now.get(13)*1000;
                long mins = now.get(12)*60000;
                long hrs = now.get(11)*3600000;
                long toNext = 86400000-(millis+secs+mins+hrs);
                Thread.sleep(toNext);  //Wait till tommorow to check again
            } catch (InterruptedException f) {
                break;
            }
        }
    });
    
    private final Runnable _startup = (() -> //LogA after system boot
    {
        long elapsed = ManagementFactory.getRuntimeMXBean().getUptime()*1000;
        try
        {
            long toGo = boot - elapsed;
            if (toGo > margin*2)
                Thread.sleep (toGo - margin);
        }
        catch (InterruptedException f) {
            //exit
        }

        while (!stop)
        {
            elapsed = ManagementFactory.getRuntimeMXBean().getUptime()*1000;
            if (elapsed >= boot && elapsed < boot+lag)
            {
               logger.Trigger("Log after startup");
               break;
            }
            else if (elapsed > boot+lag)
                break;
        }
    });
    
    private final Runnable _once = (() ->  //Log one time only
    {
        try {
            Thread.sleep (snooze (new Date(single)));
        }
        catch (InterruptedException f) {
            //exit
        }

        while (!stop)
        {
            long now = System.currentTimeMillis();
            if (now >= single && now < single+lag)
            {
               logger.Trigger("Log on date");
               break;
            }
            else if (now > single+lag)
                break;
        }
    });
    
    private final Runnable _advanced = (() ->  //Offset and Interval are set
    {
        long now, target = reset(new Date()) + offset;
        int frequency = (int)(scale/interval);

        while (!stop)
        {
            now = System.currentTimeMillis();
            try {
                Thread.sleep (snooze (new Date(target)));
            }
            catch (InterruptedException f) {
                break;
            }

            if (now >= target && now <= target+lag && daySelected && start && !end)
            {
                logger.Trigger("Scheduled log");
                target = reset(new Date()) + offset;
                try {
                    Thread.sleep (lag); //To prevent duplicate logging
                }
                catch (InterruptedException upt) {
                    break;
                }
            }
            else if (now > target+lag)
                for (int i = 0; i < frequency; i++)
                {
                    target += interval;
                    //now = System.currentTimeMillis();
                    if (target > now)
                        break;
                }
        }
    });
    
    private final Runnable _cycle = (() -> //Log once per "scale" - e.g. if Offset = "50:00", log on the 50th minute of every hour.
    {
       long target = reset(new Date()) + offset+scale;
       while (!stop)
       {
           long now = System.currentTimeMillis();
           if (now >= target && now <= target+lag && daySelected && start && !end)
           {
               logger.Trigger("Scheduled log");
               target = reset(new Date()) + offset;
               try {
                   Thread.sleep (snooze (new Date(target)));
               }
               catch (InterruptedException f) {
                   break;
               }
           }
           else if (now > target+lag)
           {
               target = reset(new Date())+offset;
               try {
                   Thread.sleep (snooze (new Date(target)));
               }
               catch (InterruptedException f) {
                   break;
               }
           }
       }
    });
    
    private final Runnable _routine = (() ->   //Interval is set, but with no reference time (Offset).
    {
        long now = System.currentTimeMillis();
        long target = now + interval;
        while (!stop)
        {
            now = System.currentTimeMillis();
            if (now >= target && now < target+lag && daySelected && start && !end)
            {
                logger.Trigger("Interval log");
                now = System.currentTimeMillis();
                target = now + interval;
                try {
                    Thread.sleep (interval - margin);
                }
                catch (InterruptedException f) {
                    break;
                }
            }
            else if (now > target+lag)
            {
                target = now+interval;
                try
                {
                    Thread.sleep(interval-margin);
                } catch (InterruptedException f)
                {
                    break;
                }
            }
        }
    });
}
