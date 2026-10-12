class CookingTask extends Thread {
    private final String task;
    private final long millis;

    CookingTask(String task, long millis) {
        this.task = task;
        this.millis = millis;
    }

    public void run() {
        System.out.println("  [" + Thread.currentThread().getName() + "] started " + task);
        try {
            Thread.sleep(millis); // simulates the time the dish takes
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("  [" + Thread.currentThread().getName() + "] finished " + task);
    }
}

public class Restaurant {
    static final String[] DISHES = {"Pasta", "Salad", "Dessert", "Rice"};
    static final long COOK_TIME_MS = 2000; // each dish takes 2 seconds

    public static void main(String[] args) throws InterruptedException {
        System.out.println("CPU cores available: " + Runtime.getRuntime().availableProcessors());

        // ---------- 1. SINGLE-THREADED ----------
        System.out.println("\n=== Single-threaded (one cook, one dish at a time) ===");
        long start = System.currentTimeMillis();
        for (String dish : DISHES) {
            // run() is called directly -> executes on the main thread, one after another
            new CookingTask(dish, COOK_TIME_MS).run();
        }
        long singleTime = System.currentTimeMillis() - start;
        System.out.println("Single-threaded total: " + singleTime + " ms");

        // ---------- 2. MULTI-THREADED ----------
        System.out.println("\n=== Multi-threaded (four cooks working at once) ===");
        start = System.currentTimeMillis();
        Thread[] threads = new Thread[DISHES.length];
        for (int i = 0; i < DISHES.length; i++) {
            threads[i] = new CookingTask(DISHES[i], COOK_TIME_MS);
            threads[i].start(); // start() -> new thread, runs concurrently
        }
        for (Thread t : threads) {
            t.join(); // wait for every cook to finish before measuring
        }
        long multiTime = System.currentTimeMillis() - start;
        System.out.println("Multi-threaded total: " + multiTime + " ms");

        // ---------- Summary ----------
        System.out.println("\n=== Summary ===");
        System.out.println("Single-threaded : " + singleTime + " ms");
        System.out.println("Multi-threaded  : " + multiTime + " ms");
        System.out.printf("Speedup         : %.1fx faster%n", (double) singleTime / multiTime);
    }
}
