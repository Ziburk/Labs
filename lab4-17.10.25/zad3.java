import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    // === Точка входа ===
    public static void main(String[] args) throws InterruptedException {
        final int N = 5;
        Fork[] forks = new Fork[N];
        for (int i = 0; i < N; i++) forks[i] = new Fork();

        // Параметры (можно вводить через Scanner)
        int thinkMinMs = 200;
        int thinkMaxMs = 800;
        int eatTimeMs = 300;
        int maxWaitToEatMs = 1200;

        AtomicBoolean gameOver = new AtomicBoolean(false);

        Philosopher[] philosophers = new Philosopher[N];
        for (int i = 0; i < N; i++) {
            Fork left = forks[i];
            Fork right = forks[(i + 1) % N];
            philosophers[i] = new Philosopher(i + 1, left, right,
                    thinkMinMs, thinkMaxMs, eatTimeMs, maxWaitToEatMs, gameOver);
        }

        // Стартуем философов
        for (Philosopher p : philosophers) p.start();

        // Ждём пока кто-то не установит gameOver
        while (!gameOver.get()) {
            Thread.sleep(200);
        }

        System.out.println("\n=== GAME OVER ===");
        for (Philosopher p : philosophers) p.interrupt();
        for (Philosopher p : philosophers) p.join();

        // === Финальная статистика ===
        System.out.println("\nСтатистика приёмов пищи:");
        int totalMeals = 0;
        for (Philosopher p : philosophers) {
            System.out.printf("%s — %d приёмов%n", p.getName(), p.meals);
            totalMeals += p.meals;
        }

        System.out.println("\nОбщее количество приёмов пищи: " + totalMeals);
    }
}

// Обычный (не static) класс Fork
class Fork {
    private boolean taken = false;

    // Пытаемся взять вилку. Возвращает true если взяли, false если таймаут.
    public synchronized boolean pick(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (taken) {
            long rem = deadline - System.currentTimeMillis();
            if (rem <= 0) return false; // не дождались
            wait(rem); // ждём, пока кто-то не положит вилку
        }
        taken = true;
        return true;
    }

    // Положить вилку и разбудить ожидающих
    public synchronized void put() {
        if (taken) {
            taken = false;
            notifyAll();
        }
    }
}

// Обычный (не static) класс Philosopher
class Philosopher extends Thread {
    private final int id;
    private final Fork left;
    private final Fork right;
    private final int thinkMinMs;
    private final int thinkMaxMs;
    private final int eatTimeMs;
    private final int maxWaitToEatMs;
    private final AtomicBoolean gameOver;
    public int meals = 0; // публично для финальной статистики
    private long lastMealTime;

    Philosopher(int id, Fork left, Fork right,
                int thinkMinMs, int thinkMaxMs, int eatTimeMs, int maxWaitToEatMs,
                AtomicBoolean gameOver) {
        super("Философ-" + id);
        this.id = id;
        this.left = left;
        this.right = right;
        this.thinkMinMs = thinkMinMs;
        this.thinkMaxMs = thinkMaxMs;
        this.eatTimeMs = eatTimeMs;
        this.maxWaitToEatMs = maxWaitToEatMs;
        this.gameOver = gameOver;
        this.lastMealTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            while (!gameOver.get()) {
                // Думает
                int think = ThreadLocalRandom.current().nextInt(thinkMinMs, thinkMaxMs + 1);
                log("думает " + think + "ms");
                sleepInterruptibly(think);

                long attemptStart = System.currentTimeMillis();
                long timeSinceLastMeal = attemptStart - lastMealTime;
                long remainingPatience = maxWaitToEatMs - timeSinceLastMeal;
                if (remainingPatience <= 0) {
                    log("не дождался еды (сразу), уходит. GAME OVER");
                    gameOver.set(true);
                    break;
                }

                boolean ate = tryEatWithin(remainingPatience);
                if (!ate) {
                    long now = System.currentTimeMillis();
                    if (now - lastMealTime >= maxWaitToEatMs) {
                        log("не дождался еды (timeout), уходит. GAME OVER");
                        gameOver.set(true);
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            log("уходит (приёмов пищи: " + meals + ")");
        }
    }

    // Пытаемся поесть в пределах totalTimeoutMs (миллисекунд)
    private boolean tryEatWithin(long totalTimeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + totalTimeoutMs;
        while (!gameOver.get()) {
            long now = System.currentTimeMillis();
            long remaining = deadline - now;
            if (remaining <= 0) return false;

            boolean leftTaken = false;
            try {
                long leftTry = Math.min(remaining, 200);
                leftTaken = left.pick(leftTry);
                if (!leftTaken) {
                    randomBackoff();
                    continue;
                }

                now = System.currentTimeMillis();
                remaining = deadline - now;
                if (remaining <= 0) {
                    left.put();
                    return false;
                }

                boolean rightTaken = right.pick(remaining);
                if (rightTaken) {
                    log("взял обе вилки и начинает есть");
                    sleepInterruptibly(eatTimeMs);
                    meals++;
                    lastMealTime = System.currentTimeMillis();
                    right.put();
                    left.put();
                    log("закончил есть (приёмов: " + meals + ")");
                    return true;
                } else {
                    left.put();
                    randomBackoff();
                }
            } catch (InterruptedException ex) {
                if (leftTaken) left.put();
                throw ex;
            }
        }
        return false;
    }

    private void randomBackoff() throws InterruptedException {
        int backoff = ThreadLocalRandom.current().nextInt(10, 80);
        Thread.sleep(backoff);
    }

    private void sleepInterruptibly(long ms) throws InterruptedException {
        long end = System.currentTimeMillis() + ms;
        while (!gameOver.get()) {
            long rem = end - System.currentTimeMillis();
            if (rem <= 0) break;
            Thread.sleep(Math.min(rem, 200));
        }
    }

    private void log(String msg) {
        System.out.printf("[%tT] %s: %s%n", System.currentTimeMillis(), getName(), msg);
    }
}
