/**
 * Continuously generates random numbers with the specified "maxValue" until "size" consecutively generated numbers are all the same value.
 * @param {Number} maxValue - the maximum value each number can be (upper limit).
 * @param {Number} size  - the number of consecutive matches required (minimum is 2).
 * @return {Promise} - a Thenable which will resolve when all numbers match. The returned result of the fulfilled promise includes the matching number, start date, end date and elpased time (in milliseconds).
 */
function randBenchmark(maxValue = Math.round(Math.random()*1000000), size = 2) {
    return new Promise (function(resolve, reject) {
        const st = new Date();
        let numbers = [], done;
        do {
            done = true;
            for (let i = 0; i < size; i++) {
                numbers[i] = Math.round(Math.random()*maxValue);
                if (i > 0 && numbers[i] != numbers[i-1]) {
                    done = false;
                    break;
                }
            }
        }
        while(!done);
        
        const et = new Date();
        resolve({
            number : numbers[0],
            startTime : st,
            endTime : et,
            elapsed : Math.abs(et.getTime() - st.getTime())
        });
    });
}

randBenchmark.apply(null, process.argv.slice(2)).then(result => {
    console.log(result.number+" in "+result.elapsed+" ms");
});
