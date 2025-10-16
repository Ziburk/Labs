import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static void main(String[] args) {
        Parking parking = new Parking(5); // максимум 5 мест

        //--------------------------------
        // Изменяемые параменты
        int totalCars = 10;
        int genFrequency = 500;
        int minTime = 5000;
        int maxTime = 10000;
        //--------------------------------

        for (int i = 1; i <= totalCars; i++) {
            new CarThread("Машина-" + i, parking, minTime, maxTime).start();

            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(genFrequency - genFrequency/2, genFrequency + genFrequency/2));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class Parking {
    private final int capacity; // Вместимость парковки
    private int carCount = 0;

    public Parking(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void enter(String carName) {
        while (carCount >= capacity) { // пока парковка заполнена
            try {
                System.out.println(carName + " ждёт место...");
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        carCount++;
        System.out.println(carName + " заехала. Занято мест: " + carCount);
    }

    public synchronized void leave(String carName) {
        carCount--;
        System.out.println(carName + " уехала. Занято мест: " + carCount);
        notify();
    }
}

class CarThread extends Thread {
    private final Parking parking;
    int minTime;
    int maxTime;

    public CarThread(String name, Parking parking, int minTime, int maxTime) {
        super(name);
        this.parking = parking;
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    @Override
    public void run() {
        parking.enter(this.getName());

        // Время на парковке
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minTime, maxTime));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        parking.leave(this.getName());
    }
}
