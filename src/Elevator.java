import java.util.*;

public class Elevator implements Runnable {
    // вмісткість
    private static final int CAPACITY = 6;
    // час проходження 1 поверху
    public static final int MSEC_PER_FLOOR = 50;
    // кількість поверхів в будинку
    private int floorsCount;
    // стартовий поверх ліфта
    private volatile int currentFloor = 1;
    // набір номерів поверхів, на які виклакано ліфт
    private Set<Integer> callFloors = new HashSet<>();
    // <Destination floor, list of passengers>
    private Map<Integer, List<Passenger>> passengers = new TreeMap<>();
    // статус ліфта
    private State state = State.STOPPED;
    // напрямок руху ліфта
    private Way way = Way.UP;
    // спостерігачі за ліфтом
    public Map<Integer, List<Passenger>> watchers = new HashMap<>();

    enum Way {
        UP, DOWN;
    }

    enum State {
        MOVING, STOPPED;
    }

    public Elevator(int floorsCount) {
        this.floorsCount = floorsCount;
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            while (state.equals(State.MOVING))
                move();
        }
    }

    // метод виклику ліфта на поверх
    public void call(int floorID, Passenger passenger) {
        if(floorID < 0 || floorID > floorsCount)
            return;
        synchronized (callFloors) {
            System.out.println("Пасажир викликав ліфт: " + passenger);
            if (state.equals(State.STOPPED))
                state = State.MOVING;
            callFloors.add(floorID);
        }
    }

    // метод перевірки статусу кнопки виклику ліфта
    public boolean checkCallButton(int floorID) {
        synchronized (callFloors) {
            if (callFloors.contains(floorID))
                return true;
            return false;
        }
    }

    // повертає скільки пасажирів в ліфті на даний момент
    private int currentPassengersCount() {
        int count = 0;
        synchronized (passengers) {
            for (Map.Entry<Integer, List<Passenger>> entry : passengers.entrySet()) {
                count += entry.getValue().size();
            }
        }
        return count;
    }

    // додати спостерігача(людину, яка очікує ліфт на своєму поверсі, тобто спостерігає за ліфтом доки він не прибуде)
    public void addWatcher(Passenger passenger) {
        synchronized(watchers) {
            if(passenger != null) {
                if(passenger.getCurrentFloor() > 0 && passenger.getFloorTo() <= floorsCount) {
                    List<Passenger> watchersOnSameFloor;
                    if(watchers.containsKey(passenger.getCurrentFloor()))
                        watchersOnSameFloor = watchers.get(passenger.getCurrentFloor());
                    else {
                        watchersOnSameFloor = new ArrayList<>();
                        watchers.put(passenger.getCurrentFloor(), watchersOnSameFloor);
                    }
                    watchersOnSameFloor.add(passenger);
                }
            }
        }
    }

    // завантажує пасажира в ліфт
    public synchronized void loadPassenger(Passenger passenger) throws ElevatorIsFullException {
        if(passenger == null)
            return;
        if(currentPassengersCount() == CAPACITY)
            throw new ElevatorIsFullException("Elevator is full");
        passenger.setState(Passenger.State.IN_PROGRESS);
        System.out.println("Пасажир зайшов в ліфт: " + passenger);
        // додаємо в пасажири
        if(passengers.containsKey(passenger.getFloorTo()))
            passengers.get(passenger.getFloorTo()).add(passenger);
        else {
            List<Passenger> passangersToSameFloor = new ArrayList<>();
            passangersToSameFloor.add(passenger);
            passengers.put(passenger.getFloorTo(), passangersToSameFloor);
        }
    }

    private void printPassengers() {
        System.out.println("\n\n===============================================");
        System.out.println("Поверх: " + currentFloor + " Пасажирів в ліфті: " + currentPassengersCount());
        System.out.println("Пасажири: {");
        passengers.values().forEach(list -> list.stream().forEach(passenger -> System.out.println(passenger)));
        System.out.println("}\n");
    }

    private void move() {
        int downDest = getDownDestination();
        int upDest = getUpDestination();
        if(downDest != 0 || upDest != 0) {
            if (way.equals(Way.UP) && currentFloor >= upDest && currentFloor > 1 && downDest != 0)
                    way = Way.DOWN;
            else if (way.equals(Way.DOWN) && (downDest == 0 || currentFloor <= downDest))
                    way = Way.UP;
            printPassengers();
            synchronized (callFloors) {
                synchronized (watchers) {
                    if (passengers.containsKey(currentFloor)
                            || (callFloors.contains(currentFloor) && currentPassengersCount() < CAPACITY)) {
                        System.out.println("Відкрити двері");
                        if (passengers.containsKey(currentFloor))
                            out(currentFloor);
                        if (callFloors.contains(currentFloor) && currentPassengersCount() < CAPACITY) {
                            in(currentFloor);
                        }
                        System.out.println("Закрити двері");
                        // скидаєм кнопку виклику ліфта
                        callFloors.remove(currentFloor);
                        // видаляєм спостерігачів на поверсі за ліфтом
                        List<Passenger> removedWatches = watchers.remove(currentFloor);
                        //System.out.println(callFloors);
                    }
                }
            }
            if(way.equals(Way.UP))
                currentFloor++;
            else
                currentFloor--;
        } else {
            System.out.println("\n\nЛіфт зупинився. Виклики відсутні. Пасажири відсутні.");
            state = State.STOPPED;
            Thread.currentThread().interrupt();
        }
        if(!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(MSEC_PER_FLOOR);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // вихід з ліфта
    private void out(int floorID) {
        List<Passenger> passengersToOut = passengers.remove(floorID);
        if (passengersToOut != null) {
            passengersToOut.stream().forEach(
                    passenger -> {
                        passenger.setCurrentFloor(floorID);
                        System.out.println("Пасажир вийшов: " + passenger);
                    }
            );
        }
    }

    // вхід в ліфт
    private void in(int floorID) {
        synchronized (watchers) {
            if (callFloors.contains(floorID)) {
                List<Passenger> watchersOnTheFloor = watchers.get(floorID);
                if (watchersOnTheFloor != null) {
                    for (Passenger passenger : watchersOnTheFloor)
                        passenger.comeIn(this);
                }
            }
        }
    }

    // повертає верхній пункт призначення(поверх)
    private int getUpDestination() {
        if(!passengers.isEmpty())
            return passengers.keySet().stream().max(Integer::compareTo).get();
        else if(!callFloors.isEmpty())
            return callFloors.stream().max(Integer::compareTo).get();
        else
            return 0;
    }

    // повертає нижній пункт призначення(поверх)
    private int getDownDestination() {
        if(!passengers.isEmpty())
            return passengers.keySet().stream().min(Integer::compareTo).get();
        else if(!callFloors.isEmpty())
            return callFloors.stream().min(Integer::compareTo).get();
        else
            return 0;
    }
}
