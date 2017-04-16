#include <stdio.h>
#include <string.h>
#include <time.h>

const int ack(const int m, const int n)
{
    int result;
    if (m == 0) result = n+1;
    else if (n == 0) result = ack(m-1, 1);
    else result = ack(m-1, ack(m, n-1));
    //printf("ack(%d, %d) = %d\n", m, n, result);
    return result;
}

int main(int argc, char* argv[])
{
    int m, n;
    if ((sscanf(argv[1], "%i", &m) != 1) || ((sscanf(argv[2], "%i", &n) != 1))) {
        fprintf(stderr, "Not an integer!");
        return -1;
    }
    const clock_t startTime = clock();
    const int result = ack(m, n);
    const clock_t endTime = clock();
    printf("\nAckermann result = %d\nTime taken = %ld ms", result, timediff(startTime, endTime));
    return 0;
}

long timediff(clock_t t1, clock_t t2) {
    long elapsed;
    elapsed = ((double) t2 - t1) / CLOCKS_PER_SEC * 1000;
    return elapsed;
}