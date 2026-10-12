# Java Multi-Threading Demo

A small command-line program that shows, with timelines and bar charts, how much faster
**multi-threaded** code can be compared to **single-threaded** code. It is written for
beginners: every part of the code has comments explaining what is happening.

The scenario is a restaurant. Each "dish" is a task, and a "cook" is a thread.

## What you will learn

| Demo | What it shows | Where you will see a speed-up |
|------|---------------|-------------------------------|
| 1. Waiting tasks | 4 tasks that `Thread.sleep` for 1 second each | On **any** PC, even with 1 core |
| 2. Heavy computing | 4 tasks that keep the CPU 100% busy | Only with **several cores** |
| 3. Scaling | One big job split between 1, 2, 4, 8, *cores* and *2 x cores* threads | Speed-up grows until you reach your core count, then stops |
| 4. Thread pool | 12 orders handled by a fixed pool of 4 reusable threads (`ExecutorService`) | Orders run in "waves" as workers become free |

## Requirements

- **JDK 11 or newer** (JDK 21 recommended). You need the JDK, not just the JRE, because the
  program is compiled with `javac`.
- Any terminal (PowerShell, Command Prompt, Windows Terminal, macOS Terminal, Linux shell).

Check your setup:

```
java -version
javac -version
```

## Run it

```
javac ThreadingDemo.java
java ThreadingDemo
```

A full run takes roughly **45 to 60 seconds** on a modern PC.

### Options

| Command | What it does |
|---------|--------------|
| `java ThreadingDemo` | Runs all 4 demos |
| `java ThreadingDemo 2` | Runs only demo number 2 (use 1, 2, 3 or 4) |
| `java ThreadingDemo --scale=0.5` | Does half as much work per task (faster run, good for slow PCs) |
| `java ThreadingDemo --scale=2` | Does double the work (more accurate numbers on fast PCs) |
| `java ThreadingDemo --plain` | Turns off colours |

Options can be combined, for example `java ThreadingDemo 3 --scale=0.5 --plain`.

If your terminal shows odd characters such as `←[31m` instead of colours, use `--plain`.

## How to read the output

**1. Log lines.** Every task prints when it starts and finishes, with the time since the run began
and the thread that ran it:

```
[ 0.00s] main               started   Pasta
[ 0.23s] main               finished  Pasta
```

- Single-threaded runs use the thread named `main` and start tasks one after another.
- Multi-threaded runs use `Thread-0`, `Thread-1`, ... and all tasks start at about `0.00s`.

**2. Timeline.** Each row is one task. The `#` characters show *when* it was running. Both
timelines use the same time scale so you can compare them directly.

- A **staircase** means the tasks ran one after another.
- Rows **lined up in the same columns** mean the tasks ran at the same time.

**3. Total time.** Two bars and a result line, in green when the speed-up is real
(1.5x or more) and yellow when it is not.

### Sample output (demo 1, trimmed)

Run with `--scale=0.15` on a single-core machine. Demo 1 gives this result on any PC.

```
 TIMELINE  (each row = one task, '#' = when it was running, same time scale for both)

  Single-threaded:
  task      thread           0s                                              0.7s
  Pasta     main             |################                                  |
  Salad     main             |                ###########                       |
  Dessert   main             |                           ###########            |
  Rice      main             |                                      ############|

  Multi-threaded:
  task      thread           0s                                              0.7s
  Pasta     Thread-0         |###########                                       |
  Salad     Thread-1         |###########                                       |
  Dessert   Thread-2         |###########                                       |
  Rice      Thread-3         |###########                                       |

 TOTAL TIME
  Single-threaded |##################################################|    679 ms
  Multi-threaded  |###########                                       |    154 ms

  Result: multi-threaded was 4.4x FASTER
```

## Key ideas

- **Thread**: an independent path of execution inside your program.
- **Single-threaded**: tasks run one after another. Calling `task.run()` directly does *not*
  create a new thread.
- **Multi-threaded**: `new Thread(task).start()` launches a new thread, and `join()` waits for it
  to finish.
- **Concurrency vs parallelism**: *concurrency* means tasks overlap in time (this works on one
  core, for example while threads wait). *Parallelism* means tasks run at the exact same
  instant on different cores (this needs several cores).
- **Waiting work vs computing work**: sleeping, network and disk waits need no CPU, so threads
  overlap them easily. Heavy maths needs a core for the whole time, so the best possible speed-up
  is roughly the number of cores.
- **Thread pool**: a fixed set of worker threads that are reused for many tasks. It avoids creating
  too many threads and is how real programs usually do it.

## Why results vary

Your numbers will differ from run to run and from PC to PC. Common reasons:

- **Core count.** The program prints it at the top (`Runtime.getRuntime().availableProcessors()`).
  This counts *logical* cores, so 12 logical cores may be only 6 physical cores plus hyperthreading.
- **Heat and power limits.** Laptops slow down when all cores are busy.
- **Other programs.** Browsers, antivirus scans and updates compete for the CPU.
- **JIT warm-up.** Java optimises code while it runs. The program does a short warm-up first, but
  small differences remain.

A speed-up of 3.8x to 4.2x for 4 tasks is normal and counts as "about 4x".

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `'javac' is not recognized` | Only a JRE is installed, or the JDK `bin` folder is not on your `PATH`. Install a JDK or add its `bin` folder to your user `PATH`. |
| Demo 2 shows no speed-up | Java is using only 1 core (a 1-core PC, a container or a CPU limit). This is expected. Demo 1 still shows the speed-up. |
| The run takes too long | Use `--scale=0.3`, or run one demo at a time (`java ThreadingDemo 1`). |
| Timings look too small or noisy | Use `--scale=2` so each task runs longer. |
| Strange characters instead of colours | Use `--plain`. |

## Files

| File | Description |
|------|-------------|
| `ThreadingDemo.java` | The full demo (all 4 examples, timelines and charts) |
| `Restaurant.java` | The first, minimal example: 4 threads printing a message |
| `HeavyRestaurant.java` | A simpler heavy-computing comparison without the charts |

The last two files are optional starting points. `ThreadingDemo.java` is self-contained and
does not depend on them.
