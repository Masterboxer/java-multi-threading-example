
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ThreadingDemo - see the difference between single-threaded and multi-threaded
 * code.
 *
 * HOW TO RUN javac ThreadingDemo.java java ThreadingDemo (runs all 4 demos,
 * takes about 40-60 seconds) java ThreadingDemo 2 (runs only demo 2) java
 * ThreadingDemo --plain (no colours, use this if you see strange characters)
 * java ThreadingDemo --scale=0.5 (half the work, for slower PCs; 2 = double the
 * work)
 *
 * THE 4 DEMOS 1. Waiting tasks (Thread.sleep) -> threads help on ANY PC, even
 * with 1 core 2. Heavy computing tasks -> threads help only if you have several
 * cores 3. Splitting one big job -> how speed grows as you add threads 4.
 * Thread pool (ExecutorService) -> the professional way to manage threads
 *
 * HOW TO READ THE OUTPUT - Every line shows a time stamp like [ 1.02s] =
 * seconds since that run started. - The TIMELINE draws each task as a row of
 * '#'. Rows that sit one below another in a staircase = tasks run one after
 * another. Rows that line up in the same columns = tasks run at the same time.
 */
public class ThreadingDemo {

    // ------------------------------------------------------------------
    // Settings and shared variables
    // ------------------------------------------------------------------
    // Multiplies how much work every task does. Change it with --scale=0.5 etc.
    static double scale = 1.0;

    // How wide (in characters) the bars and timelines are.
    static final int BAR_WIDTH = 50;

    // The heavy loop stores its answer here. If we threw the answer away, Java's JIT
    // compiler might notice the loop is pointless and delete it, which would ruin the test.
    static volatile double sink;

    // The moment the current run started (measured with System.nanoTime()).
    static long t0;

    // Colour codes. They stay empty ("") unless colours are turned on in main().
    static String RESET = "", BOLD = "", DIM = "", RED = "", GREEN = "", YELLOW = "", CYAN = "";

    static void enableColors() {
        RESET = "\u001B[0m";
        BOLD = "\u001B[1m";
        DIM = "\u001B[2m";
        RED = "\u001B[31m";
        GREEN = "\u001B[32m";
        YELLOW = "\u001B[33m";
        CYAN = "\u001B[36m";
    }

    // Milliseconds since the current run started.
    static long elapsedMs() {
        return (System.nanoTime() - t0) / 1_000_000;
    }

    // ------------------------------------------------------------------
    // main: reads the options, then runs the demos
    // ------------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        int onlyDemo = 0;        // 0 means "run every demo"
        boolean useColor = true;

        for (String arg : args) {
            if (arg.equals("--plain")) {
                useColor = false;
            } else if (arg.startsWith("--scale=")) {
                scale = Double.parseDouble(arg.substring("--scale=".length()));
            } else {
                onlyDemo = Integer.parseInt(arg);
            }
        }
        if (useColor) {
            enableColors();
        }

        // How many tasks can truly run at the same instant on this PC?
        int cores = Runtime.getRuntime().availableProcessors();

        printBanner(cores);
        warmUp();

        if (onlyDemo == 0 || onlyDemo == 1) {
            demo1Waiting();
        }
        if (onlyDemo == 0 || onlyDemo == 2) {
            demo2Computing();
        }
        if (onlyDemo == 0 || onlyDemo == 3) {
            demo3Scaling(cores);
        }
        if (onlyDemo == 0 || onlyDemo == 4) {
            demo4ThreadPool(cores);
        }

        printTakeaways(cores);
    }

    // ------------------------------------------------------------------
    // DEMO 1: tasks that spend their time WAITING
    // ------------------------------------------------------------------
    static void demo1Waiting() throws InterruptedException {
        header("DEMO 1 - Waiting tasks (Thread.sleep)",
                "4 dishes, each takes 1 second of waiting. The CPU is idle while they wait.");

        String[] dishes = {"Pasta", "Salad", "Dessert", "Rice"};
        long cookMs = Math.round(1000 * scale);

        System.out.println();
        System.out.println(RED + BOLD + " SINGLE-THREADED" + RESET
                + " - one cook makes the dishes one after another");
        List<CookingTask> single = makeTasks(CookingTask.Kind.WAIT, dishes, cookMs, false);
        long singleMs = runSingleThreaded(single);

        System.out.println();
        System.out.println(GREEN + BOLD + " MULTI-THREADED" + RESET
                + " - four cooks work at the same time");
        List<CookingTask> multi = makeTasks(CookingTask.Kind.WAIT, dishes, cookMs, false);
        long multiMs = runMultiThreaded(multi);

        showComparison(single, singleMs, multi, multiMs);
        System.out.println(DIM + " Why it is faster: while one thread sleeps, the others can sleep too."
                + RESET);
        System.out.println(DIM + " This works even on a 1-core PC, because sleeping needs no CPU." + RESET);
    }

    // ------------------------------------------------------------------
    // DEMO 2: tasks that need the CPU the whole time
    // ------------------------------------------------------------------
    static void demo2Computing() throws InterruptedException {
        header("DEMO 2 - Heavy computing tasks (a big maths loop)",
                "4 dishes, each keeps the CPU 100% busy. Speed-up needs several cores.");

        String[] dishes = {"Pasta", "Salad", "Dessert", "Rice"};
        long iterations = Math.round(40_000_000L * scale);

        System.out.println();
        System.out.println(RED + BOLD + " SINGLE-THREADED" + RESET
                + " - one core does all the maths, one task after another");
        List<CookingTask> single = makeTasks(CookingTask.Kind.COMPUTE, dishes, iterations, false);
        long singleMs = runSingleThreaded(single);

        System.out.println();
        System.out.println(GREEN + BOLD + " MULTI-THREADED" + RESET
                + " - four threads, the OS can put them on different cores");
        List<CookingTask> multi = makeTasks(CookingTask.Kind.COMPUTE, dishes, iterations, false);
        long multiMs = runMultiThreaded(multi);

        double speedup = showComparison(single, singleMs, multi, multiMs);
        if (speedup >= 1.5) {
            System.out.println(DIM + " Why it is faster: each thread gets its own core, so the maths runs in parallel."
                    + RESET);
        } else {
            System.out.println(YELLOW + " Almost no speed-up: Java can only use 1 core here, so the threads just took turns."
                    + RESET);
        }
        System.out.println(DIM + " Computing work can only go as fast as the number of cores allows." + RESET);
    }

    // ------------------------------------------------------------------
    // DEMO 3: one big job split between 1, 2, 4, 8 ... threads
    // ------------------------------------------------------------------
    static void demo3Scaling(int cores) throws InterruptedException {
        header("DEMO 3 - Splitting ONE big job between more and more threads",
                "Same total work every time. We only change how many threads share it.");

        long totalIterations = Math.round(240_000_000L * scale);

        // A TreeSet keeps the numbers sorted and removes duplicates automatically.
        TreeSet<Integer> threadCounts = new TreeSet<>();
        threadCounts.add(1);
        threadCounts.add(2);
        threadCounts.add(4);
        threadCounts.add(8);
        threadCounts.add(cores);       // exactly as many threads as cores
        threadCounts.add(cores * 2);   // twice as many threads as cores

        System.out.println();
        System.out.println(" Running... (each line appears when that run finishes)");
        System.out.println();
        System.out.println(String.format("  %7s  %-" + (BAR_WIDTH + 2) + "s %9s  %7s",
                "Threads", " Time (shorter is better)", "Time", "Speedup"));

        long baselineMs = 0;   // the time with 1 thread; everything else is compared to it
        for (int n : threadCounts) {
            // Split the total work into n equal pieces, one piece per thread.
            String[] names = numberedNames("Chunk ", n);
            List<CookingTask> tasks = makeTasks(CookingTask.Kind.COMPUTE, names,
                    totalIterations / n, true);
            long ms = runMultiThreaded(tasks);

            if (n == 1) {
                baselineMs = ms;
            }
            double speedup = (double) baselineMs / ms;

            String note = "";
            if (n == cores) {
                note = "<- same as your core count";
            } else if (n > cores) {
                note = "<- more threads than cores: no extra gain";
            }
            String color = speedup >= 1.5 ? GREEN : YELLOW;

            System.out.println(String.format("  %7d  |%s| %6d ms  %s%6.1fx%s  %s",
                    n, bar(ms, baselineMs, color), ms, color, speedup, RESET, note));
        }

        System.out.println();
        System.out.println(DIM + " Notice: the speed-up grows with the thread count until you reach the number"
                + RESET);
        System.out.println(DIM + " of cores (" + cores + " on this PC). After that, extra threads just wait for a free core."
                + RESET);
    }

    // ------------------------------------------------------------------
    // DEMO 4: a thread pool (ExecutorService)
    // ------------------------------------------------------------------
    static void demo4ThreadPool(int cores) throws InterruptedException {
        // A pool never uses more than "poolSize" threads, however many tasks you give it.
        int poolSize = Math.max(2, Math.min(4, cores));
        int orderCount = 12;
        long iterations = Math.round(20_000_000L * scale);

        header("DEMO 4 - Thread pool (ExecutorService)",
                orderCount + " restaurant orders, but only " + poolSize
                + " cooks (threads). Extra orders wait in a queue.");

        String[] orders = numberedNames("Order ", orderCount);

        System.out.println();
        System.out.println(RED + BOLD + " SINGLE-THREADED" + RESET + " - one cook, " + orderCount + " orders in a row");
        List<CookingTask> single = makeTasks(CookingTask.Kind.COMPUTE, orders, iterations, false);
        long singleMs = runSingleThreaded(single);

        System.out.println();
        System.out.println(GREEN + BOLD + " THREAD POOL" + RESET + " - " + poolSize
                + " cooks share the " + orderCount + " orders");
        List<CookingTask> pooled = makeTasks(CookingTask.Kind.COMPUTE, orders, iterations, false);

        t0 = System.nanoTime();
        // Create a pool with a fixed number of worker threads.
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        for (CookingTask task : pooled) {
            pool.submit(task);   // hand the task to the pool; a free worker will pick it up
        }
        pool.shutdown();         // "no more new tasks, finish the ones you have"
        pool.awaitTermination(10, TimeUnit.MINUTES);   // wait until everything is done
        long poolMs = elapsedMs();

        showComparison(single, singleMs, pooled, poolMs);
        System.out.println(DIM + " Look at the thread names: the same " + poolSize
                + " workers (pool-1-thread-1 ...) are reused for many orders." + RESET);
        System.out.println(DIM + " Creating a new Thread for every task is wasteful; pools reuse them." + RESET);
    }

    // ------------------------------------------------------------------
    // Running tasks: single-threaded vs multi-threaded
    // ------------------------------------------------------------------
    // Runs the tasks one after another on the CURRENT thread (main). Returns total ms.
    static long runSingleThreaded(List<CookingTask> tasks) {
        t0 = System.nanoTime();
        for (CookingTask task : tasks) {
            // Calling run() directly is a normal method call. It does NOT create a new
            // thread, so the next task cannot start until this one has finished.
            task.run();
        }
        return elapsedMs();
    }

    // Runs every task on its OWN new thread, all at once. Returns total ms.
    static long runMultiThreaded(List<CookingTask> tasks) throws InterruptedException {
        t0 = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (CookingTask task : tasks) {
            Thread thread = new Thread(task);   // wrap the task in a new thread
            threads.add(thread);
            thread.start();   // start() launches the thread and returns immediately
        }
        for (Thread thread : threads) {
            thread.join();    // wait here until that thread has finished
        }
        return elapsedMs();
    }

    // ------------------------------------------------------------------
    // The work itself
    // ------------------------------------------------------------------
    // A heavy loop that keeps the CPU busy and never waits.
    static double crunch(long iterations) {
        double sum = 0;
        for (long i = 1; i <= iterations; i++) {
            sum += Math.sqrt(i) * Math.sin(i);
        }
        return sum;
    }

    // Java optimises hot code while it runs (JIT). We run the loop briefly first so
    // the first real test is not unfairly slow.
    static void warmUp() {
        System.out.print(" Warming up... ");
        for (int i = 0; i < 3; i++) {
            sink = crunch(5_000_000L);
        }
        System.out.println("done.");
    }

    // Builds a fresh list of tasks (a task remembers its own timing, so never reuse one).
    static List<CookingTask> makeTasks(CookingTask.Kind kind, String[] names, long amount, boolean quiet) {
        List<CookingTask> tasks = new ArrayList<>();
        for (String name : names) {
            tasks.add(new CookingTask(name, kind, amount, quiet));
        }
        return tasks;
    }

    // Makes names like "Order 1", "Order 2", ...
    static String[] numberedNames(String prefix, int count) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = prefix + (i + 1);
        }
        return names;
    }

    // ------------------------------------------------------------------
    // Drawing things in the console
    // ------------------------------------------------------------------
    static void printBanner(int cores) {
        System.out.println();
        System.out.println(CYAN + BOLD + "  +----------------------------------------------------+" + RESET);
        System.out.println(CYAN + BOLD + "  |   SINGLE-THREADED  vs  MULTI-THREADED  demo        |" + RESET);
        System.out.println(CYAN + BOLD + "  +----------------------------------------------------+" + RESET);
        System.out.println("  CPU cores available to Java : " + BOLD + cores + RESET);
        System.out.println("  Work scale                  : " + scale + DIM + "  (change with --scale=0.5)" + RESET);
        System.out.println();
    }

    static void header(String title, String subtitle) {
        System.out.println();
        System.out.println(BOLD + "=".repeat(78) + RESET);
        System.out.println(BOLD + " " + title + RESET);
        System.out.println(DIM + " " + subtitle + RESET);
        System.out.println(BOLD + "=".repeat(78) + RESET);
    }

    // A bar made of '#' characters. A full-width bar means "value equals max".
    static String bar(long value, long max, String color) {
        if (max < 1) {
            max = 1;
        }
        int filled = (int) Math.max(1, Math.min(BAR_WIDTH, value * BAR_WIDTH / max));
        return color + "#".repeat(filled) + RESET + " ".repeat(BAR_WIDTH - filled);
    }

    // Draws one row per task. The position of the '#' shows WHEN the task was running.
    static void printTimeline(List<CookingTask> tasks, long scaleMs, String color) {
        for (CookingTask task : tasks) {
            int from = (int) Math.min(BAR_WIDTH - 1, task.startMs * BAR_WIDTH / scaleMs);
            int to = (int) Math.min(BAR_WIDTH, Math.max(from + 1, task.endMs * BAR_WIDTH / scaleMs));

            String row = " ".repeat(from) + color + "#".repeat(to - from) + RESET
                    + " ".repeat(BAR_WIDTH - to);
            System.out.println(String.format("  %-9s %-16s |%s|", task.name, task.threadName, row));
        }
    }

    // Prints the timelines and a total-time bar chart for the two runs.
    // Returns the speed-up factor (single time divided by multi time).
    static double showComparison(List<CookingTask> single, long singleMs,
            List<CookingTask> multi, long multiMs) {
        long scaleMs = Math.max(1, Math.max(singleMs, multiMs));

        System.out.println();
        System.out.println(BOLD + " TIMELINE" + RESET + DIM
                + "  (each row = one task, '#' = when it was running, same time scale for both)" + RESET);

        String prefix = String.format("  %-9s %-16s ", "task", "thread");
        String axisEnd = String.format("%.1fs", scaleMs / 1000.0);
        String axis = prefix + "0s" + " ".repeat(Math.max(1, BAR_WIDTH - 2 - axisEnd.length() + 2)) + axisEnd;

        System.out.println();
        System.out.println(RED + "  Single-threaded:" + RESET);
        System.out.println(DIM + axis + RESET);
        printTimeline(single, scaleMs, RED);

        System.out.println();
        System.out.println(GREEN + "  Multi-threaded:" + RESET);
        System.out.println(DIM + axis + RESET);
        printTimeline(multi, scaleMs, GREEN);

        System.out.println();
        System.out.println(BOLD + " TOTAL TIME" + RESET);
        System.out.println(String.format("  Single-threaded |%s| %6d ms", bar(singleMs, scaleMs, RED), singleMs));
        System.out.println(String.format("  Multi-threaded  |%s| %6d ms", bar(multiMs, scaleMs, GREEN), multiMs));

        double speedup = (double) singleMs / Math.max(1, multiMs);
        String verdict;
        if (speedup >= 1.5) {
            verdict = GREEN + BOLD + String.format("%.1fx FASTER", speedup) + RESET;
        } else {
            verdict = YELLOW + BOLD + String.format("%.1fx (no real speed-up)", speedup) + RESET;
        }
        System.out.println();
        System.out.println("  Result: multi-threaded was " + verdict);
        System.out.println();
        return speedup;
    }

    static void printTakeaways(int cores) {
        header("WHAT TO TAKE AWAY", "");
        System.out.println(" 1. Waiting work (sleep, network, files): threads help on every PC.");
        System.out.println(" 2. Computing work: the best speed-up is about the number of cores (" + cores + " here).");
        System.out.println(" 3. More threads than cores does not make computing faster.");
        System.out.println(" 4. A thread pool reuses a fixed number of threads, which is how real programs do it.");
        System.out.println();
    }
}

// ----------------------------------------------------------------------
// One "dish" to cook. It implements Runnable, which just means:
// "this object has a run() method that can be executed by a thread".
// ----------------------------------------------------------------------
class CookingTask implements Runnable {

    // The two kinds of work: waiting (sleep) or computing (busy loop).
    enum Kind {
        WAIT, COMPUTE
    }

    final String name;      // e.g. "Pasta"
    final Kind kind;
    final long amount;      // milliseconds to wait, OR number of loop iterations
    final boolean quiet;    // true = do not print start/finish lines

    // Filled in while the task runs, so we can draw the timeline afterwards.
    long startMs;
    long endMs;
    String threadName = "-";

    CookingTask(String name, Kind kind, long amount, boolean quiet) {
        this.name = name;
        this.kind = kind;
        this.amount = amount;
        this.quiet = quiet;
    }

    // This is the code that runs when a thread executes the task.
    @Override
    public void run() {
        threadName = Thread.currentThread().getName();   // which thread is running me?
        startMs = ThreadingDemo.elapsedMs();
        log("started", ThreadingDemo.YELLOW);

        if (kind == Kind.WAIT) {
            try {
                Thread.sleep(amount);   // pretend to cook: just wait, no CPU needed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            ThreadingDemo.sink = ThreadingDemo.crunch(amount);   // really use the CPU
        }

        endMs = ThreadingDemo.elapsedMs();
        log("finished", ThreadingDemo.GREEN);
    }

    // Prints a line like:  [ 1.02s] Thread-0           finished  Pasta
    private void log(String what, String color) {
        if (quiet) {
            return;
        }
        String time = String.format("%5.2fs", ThreadingDemo.elapsedMs() / 1000.0);
        String line = String.format("  [%s] %-18s %-9s %s", time, threadName, what, name);
        System.out.println(color + line + ThreadingDemo.RESET);
    }
}
