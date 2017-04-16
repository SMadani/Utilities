import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

class Ackermann {
    static int ack(final int m, final int n)
    {
        if (m == 0) return n+1;
        if (n == 0) return ack(m-1, 1);
        return ack(m-1, ack(m, n-1));
    }

    public static void main(String[] args)
    {
        final int m, n;
        try {
            m = Integer.parseInt(args[0]);
            n = Integer.parseInt(args[1]);
        }
        catch (Exception ex) {
            System.err.println("Both arguments must be integers.");
            return;
        }
        final long endTime, startTime = System.nanoTime();
        final int result = ack(m, n);
        endTime = System.nanoTime();
        System.out.println("ackermann("+m+", "+n+") = "+result);
        System.out.println("Time taken = "+formatDuration(Duration.ofNanos(endTime-startTime)));
    }

    static String formatDuration(Duration duration) {
    	String pattern = "";
    	if (duration.toHours() >= 1)
    		pattern += "H:";
    	if (duration.toMinutes() >= 1)
    		pattern += "m:";
        pattern += "s.SSS";
        return LocalTime.MIDNIGHT.plus(duration).format(DateTimeFormatter.ofPattern(pattern));
    }
}