import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        //--------------------------------------
        // Изменяемые параметры
        int totalShips = 20;
        int minGenFreq = 300;  // min задержка между генерациями
        int maxGenFreq = 400; // max задержка
        int unloadTime = 1000; // время разгрузки
        //--------------------------------------

        Bay bay = new Bay();

        ShipGenerator generator = new ShipGenerator(bay, totalShips, minGenFreq, maxGenFreq);

        Pier pierBread = new Pier(bay, CargoType.BREAD, unloadTime);
        Pier pierBanana = new Pier(bay, CargoType.BANANAS, unloadTime);
        Pier pierClothes = new Pier(bay, CargoType.CLOTHES, unloadTime);


        pierBread.start();
        pierBanana.start();
        pierClothes.start();
        generator.start();

        generator.join();
        pierBread.join();
        pierBanana.join();
        pierClothes.join();

        System.out.println("Конец...");
    }

}

enum CargoType {
    BREAD, BANANAS, CLOTHES
}

class Ship {
    private String name;
    private CargoType cargoType;
    private int cargoCount;

    public Ship(String name, CargoType cargoType, int cargoCount) {
        this.name = name;
        this.cargoType = cargoType;
        this.cargoCount = cargoCount;
    }

    public String getName() {
        return name;
    }

    public CargoType getCargoType() {
        return cargoType;
    }

    public int getCargoCount() {
        return cargoCount;
    }

    @Override
    public String toString() {
        return "| " + name + " | Груз: " + cargoType + " | Кол-во: " + cargoCount + " |";
    }

}

class Bay { // Бухта

    private final int capacity = 5;
    private final LinkedList<Ship> queue = new LinkedList<>();
    private boolean finished = false;
    public synchronized void enterBay(Ship ship) {
        while (queue.size() >= capacity) {
            try {
                System.out.println(ship.getName() + " ждёт входа в бухту.");
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        queue.addLast(ship);
        System.out.println(ship + " вошёл в бухту. (в бухте " + queue.size() + " кораблей)");
        notifyAll();
    }

    public synchronized Ship takeByType(CargoType type) {
        while (true) {
            // попытка найти корабль нужного типа
            for (Iterator<Ship> it = queue.iterator(); it.hasNext(); ) {
                Ship ship = it.next();
                if (ship.getCargoType() == type) {
                    it.remove();
                    System.out.println(ship + " выехал к причалу (" + type + "). (в бухте " + queue.size() + " кораблей)");
                    notifyAll();
                    return ship;
                }
            }


            if (finished && queue.isEmpty()) {
                // Новых кораблей не ожидается и буфер пуст — можно завершать причал
                return null;
            }

            // иначе ждём появления подходящего корабля или сигнала окончания
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    // Генератор сообщает, что больше кораблей не будет
    public synchronized void setFinished() {
        finished = true;
        notifyAll(); // разбудить все причалы и генератор
    }
}

class ShipGenerator extends Thread {

    private Bay bay;
    private int totalShips;
    private int minGenFreq;
    private int maxGenFreq;

    ShipGenerator(Bay bay, int totalShips, int minGenFreq, int maxGenFreq) {
        super("Generator");
        this.bay = bay;
        this.totalShips = totalShips;
        this.minGenFreq = minGenFreq;
        this.maxGenFreq = maxGenFreq;
    }

    private Ship generateRandomShip(int i) {
        CargoType[] values = CargoType.values();
        CargoType randomType = values[ThreadLocalRandom.current().nextInt(values.length)];
        int cargoAmount = ThreadLocalRandom.current().nextInt(1, 6); // 1..5 включительно
        return new Ship("Корабль-" + i, randomType, cargoAmount);
    }

    @Override
    public void run() {
        for (int i = 1; i <= totalShips; i++) {
            Ship ship = generateRandomShip(i);
            bay.enterBay(ship);

            int delay = ThreadLocalRandom.current().nextInt(minGenFreq, maxGenFreq + 1);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Генератор создал все корабли, завершение работы...");
        bay.setFinished();
    }

}

class Pier extends Thread {
    private final Bay bay;
    private final CargoType type;
    private final int unloadTime;

    Pier(Bay bay, CargoType type, int unloadTime) {
        super("Причал для разгрузки " + type.name());
        this.bay = bay;
        this.type = type;
        this.unloadTime = unloadTime;
    }

    @Override
    public void run() {
        while (true) {
            Ship s = bay.takeByType(type);
            if (s == null) {
                System.out.println(getName() + ": нет больше кораблей, завершение работы.");
                break;
            }

            // Обработка (разгрузка): время пропорционально количеству units
            int totalUnloadMs = s.getCargoCount() * unloadTime;
            System.out.println(getName() + " начал разгрузку " + s);
            try {
                Thread.sleep(totalUnloadMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            System.out.println(getName() + " завершил разгрузку " + s + " и корабль ушёл.");
        }
    }
}
