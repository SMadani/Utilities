import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MergeSortTask<T extends Comparable<T>> extends RecursiveTask<List<T>> {

    public static void main(String[] args) {
        var data = IntStream.range(0, 100).boxed().collect(Collectors.toList());
        Collections.shuffle(data);
        System.out.println("Input: ");
        System.out.println(data);
        System.out.println();
        var mst = new MergeSortTask<>(data);
        var sorted = mst.compute();
        System.out.println("Output: ");
        System.out.println(sorted);
    }

    protected final List<T> items;

    protected MergeSortTask(List<T> items) {
        this.items = items;
    }

    @Override
    public List<T> compute() {
        var a = items;
        int n = a.size();
        if (n < 2) {
            return a;
        }
        int mid = n / 2;

        var leftSort = new MergeSortTask<>(a.subList(0, mid));
        leftSort.fork();
        var rightSort = new MergeSortTask<>(a.subList(mid, n));
        rightSort.fork();

        return merge(leftSort.join(), rightSort.join());
    }

    protected List<T> merge(List<T> left, List<T> right) {
        int li = 0, ri = 0, ls = left.size(), rs = right.size();
        var result = new ArrayList<T>(ls + rs);

        while (li < ls && ri < rs) {
            T l = left.get(li), r = right.get(ri);
            if (l.compareTo(r) < 0) {
                result.add(l);
                li++;
            }
            else {
                result.add(r);
                ri++;
            }
        }
        if (li < ls) {
            result.addAll(left.subList(li, ls));
        }
        if (ri < rs) {
            result.addAll(right.subList(ri, rs));
        }

        return result;
    }
}
