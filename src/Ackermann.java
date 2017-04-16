class Ackermann {
    static int ack(final int m, final int n)
    {
        final int result;
        if (m == 0) result = n+1;
        else if (n == 0) result = ack(m-1, 1);
        else result = ack(m-1, ack(m, n-1));
        //System.out.printf("ack(%d, %d) = %d\n", m, n, result);
        return result;
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
        System.out.printf("\nAckermann result = %d\nTime taken = %d ms", result, (endTime-startTime)/1000000);
    }
}