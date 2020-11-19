import java.util.Arrays;
import java.util.Random;

public class BinarySearch implements Runnable {

    public static void main(String... args) {
        int length = 100_000;
        int index = new Random().nextInt(length - 1);
        new BinarySearch(length, index).run();
    }

    public final boolean[] flags;
    public final int expectedIndex;

    public BinarySearch(int length, int index) {
        flags = new boolean[length];
        this.expectedIndex = index - 1;
        if (index <= length) {
            Arrays.fill(flags, index, length-1, true);
        }
    }

    @Override
    public void run() {
        int actualIndex = isBadIterative();//isBadRecursive(flags, 0);
        System.out.println("Expected: "+expectedIndex);
        System.out.println("Actual: "+actualIndex);
    }

    protected int isBadIterative() {
        int currentIndex = 0;
        for (int size = flags.length / 2; size >= 1; size /= 2) {
            int mid = currentIndex + size;
            if (!flags[mid]) {
                currentIndex = mid;
            }
        }
        return currentIndex;
    }

    protected int isBadRecursive(boolean[] arr, int currentIndex) {
        if (arr.length == 1) return arr[0];
        int midIndex = arr.length/2;
        boolean midValue = arr[midIndex];
        boolean[] next;
        if (midValue) {
            next = Arrays.copyOfRange(arr, 0, midIndex);
        }
        else {
            next = Arrays.copyOfRange(arr, midIndex, arr.length);
            currentIndex += midIndex;
        }
        return isBadRecursive(next, currentIndex);
    }
}