class HeavyCookingTask extends Thread {
    private final String task;
    private final long iterations;
    private double result; // storing the result stops the JIT from deleting the loop

    HeavyCookingTask(String task, long iterations) {
        this.task = task;
        this.iterations = iterations;
    }

    public void run() {
        System.out.println("  [" + Thread.currentThread().getName() + "] started " + task);
        double sum = 0;
        for (long i = 1; i <= iterations; i++) {
            sum += Math.sqrt(i) * Math.sin(i); // real CPU work, no waiting
        }
        result = sum;
        System.out.println("  [" + Thread.currentThread().getName() + "] finished " + task);
    }

    double getResult() {
        return result;
    }
}

public class HeavyRestaurant {
    static final String[] DISHES = {"Pasta", "Salad", "Dessert", "Rice"};

    // 1 million iterations finishes in a few milliseconds, which is too fast to
    // measure reliably. Increase or decrease this to suit your machine.
    static final long ITERATIONS = 100_000_000L;

    public static void main(String[] args) throws InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("CPU cores available: " + cores);
        System.out.println("Iterations per dish: " + ITERATIONS);

        // Warm-up so the JIT compiler has optimised the loop before we time anything
        new HeavyCookingTask("warm-up", 5_000_000L).run();
        System.out.println();

        // ---------- 1. SINGLE-THREADED ----------
        System.out.println("=== Single-threaded ===");
        long start = System.currentTimeMillis();
        for (String dish : DISHES) {
            new HeavyCookingTask(dish, ITERATIONS).run(); // runs on main thread
        }
        long singleTime = System.currentTimeMillis() - start;
        System.out.println("Single-threaded total: " + singleTime + " ms");

        // ---------- 2. MULTI-THREADED ----------
        System.out.println("\n=== Multi-threaded ===");
        start = System.currentTimeMillis();
        HeavyCookingTask[] threads = new HeavyCookingTask[DISHES.length];
        for (int i = 0; i < DISHES.length; i++) {
            threads[i] = new HeavyCookingTask(DISHES[i], ITERATIONS);
            threads[i].start();
        }
        for (HeavyCookingTask t : threads) {
            t.join();
        }
        long multiTime = System.currentTimeMillis() - start;
        System.out.println("Multi-threaded total: " + multiTime + " ms");

        // ---------- Summary ----------
        System.out.println("\n=== Summary ===");
        System.out.println("Single-threaded : " + singleTime + " ms");
        System.out.println("Multi-threaded  : " + multiTime + " ms");
        System.out.printf("Speedup         : %.2fx%n", (double) singleTime / multiTime);
        System.out.println("(Best possible with " + cores + " core(s) and "
                + DISHES.length + " tasks: about "
                + Math.min(cores, DISHES.length) + "x)");
    }
}
