import java.util.stream.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import static java.lang.Thread.currentThread;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import static java.lang.System.*;

class PerformanceTest {
    public static void main(String... args) throws Exception {
    	
		boolean log = args.length > 2 ? Boolean.valueOf(args[2]) : true;
    	int numThreads = Runtime.getRuntime().availableProcessors(),
    		target = (int) Math.round(Math.random()*1024);
    	
        if (args.length > 0) {
        	int desiredTarget = Integer.parseInt(args[0]);
        	if (desiredTarget <= 0) {
                err.println("Target must be between 1 and 2147483647");
                return;
            }
        	else
        		target = desiredTarget;
        }
        if (args.length > 1) try {
        	int desiredThreads = Integer.parseInt(args[1]);
        	if (desiredThreads >= 0)
        		numThreads = desiredThreads;
        }
        catch (NumberFormatException nan) {
        	System.err.println("Number of threads must be a positive integer.");
        	return;
        }

        
		doStuff(target, new ForkJoinPool(numThreads), log);
    }

	static void doStuff(final int target, final Executor executor, final boolean log) throws Exception {
		CompletableFuture<Void> promise = CompletableFuture
			.supplyAsync(() -> compute(target, log), executor)
			.thenApply(Duration::ofNanos)
			.thenApply(PerformanceTest::formatDuration)
			.thenAccept(System.out::println)
			.exceptionally(throwable -> handleException(throwable));
		
		for (boolean b = true; !promise.isDone(); b = !b) {
			out.println(b ? "tick" : "tock");
			Thread.sleep(1000);
		}
	}

	private static Void handleException(Throwable throwable) {
		System.err.println(throwable.getMessage());
		return null;
	}

	static long compute(final int target, boolean log) {
		final long startTime = nanoTime();
		IntStream.range(0, target)
		.parallel()
		.forEach(loop -> {
			final int result = ThreadLocalRandom.current()
				.ints(0, 2147483647)
				.filter(i -> i <= target)
				.findAny()
				.getAsInt();
			if (log)
				out.println("[Result: "+result+", Iteration: "+loop+", Thread: "+currentThread().getName()+']');
		});
		return nanoTime()-startTime;
	}
    
    static String formatDuration(Duration duration) {
    	String pattern = "";
    	if (duration.toHours() >= 1)
    		pattern += "H:";
    	if (duration.toMinutes() >= 1)
    		pattern += "m:";
    	if (duration.toNanos()/1_000_000_000 >= 1)
    		pattern += "s.";
    	pattern += "SSS";
        return LocalTime.MIDNIGHT.plus(duration).format(DateTimeFormatter.ofPattern(pattern));
    }
}