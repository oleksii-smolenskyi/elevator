import java.util.*;

public class Building {
    public static final int MIN_FLOORS_COUNT = 5;
    public static final int MAX_FLOORS_COUNT = 30;
    public static final int MAX_PEOPLE_ON_FLOOR = 8;
    private static final Random RANDOM = new Random();

    private Elevator elevator;
    private Thread elevatorThread;
    private Map<Integer, List<Passenger>> passengersOnTheFloor;

    private int floorsCount;

    public Building() {
        // генеруєм кількість поверхів
        this.floorsCount = RANDOM.nextInt(Building.MAX_FLOORS_COUNT + 1 - Building.MIN_FLOORS_COUNT) + Building.MIN_FLOORS_COUNT;
        System.out.println("Всього поверхів: " + floorsCount);
        elevator = new Elevator(floorsCount);
        //генеруємо пасажирів
        Map<Integer, List<Passenger>> passangersOnTheFloor = new HashMap<>();
        for(int i = 1; i <= this.floorsCount; i++) {
            List<Passenger> passengers = new ArrayList<>();
            int peopleCount = RANDOM.nextInt(Building.MAX_PEOPLE_ON_FLOOR + 1);
            for(int p = 0; p < peopleCount; p++) {
                int floorTo = RANDOM.nextInt(this.floorsCount) + 1;
                while (floorTo == i) {
                    floorTo = RANDOM.nextInt(this.floorsCount) + 1;
                }
                Passenger passenger = new Passenger(i, floorTo);
                passengers.add(passenger);
            }
            passangersOnTheFloor.put(i, passengers);
        }
        passangersOnTheFloor.values().stream().forEach(list -> list.stream().forEach(passenger -> passenger.setWhereIAm(this)));
        System.out.println("Згенеровані пасажири: \n");
        passangersOnTheFloor.values().stream().forEach(list -> list.stream().forEach(passenger -> System.out.println(passenger)));
        this.setPeopleOnFloors(passangersOnTheFloor);
        turnOnElevator();
    }

    public Elevator getElevator() {
        return elevator;
    }

    private void turnOnElevator() {
        elevatorThread = new Thread(elevator);
        elevatorThread.start();
    }

    private void setPeopleOnFloors(Map<Integer, List<Passenger>> passangersOnTheFloor) {
        this.passengersOnTheFloor = passangersOnTheFloor;
        passangersOnTheFloor.values()
                .stream()
                .forEach(list -> list.stream().forEach(passenger -> new Thread(passenger).start()));
    }

    public Map<Integer, List<Passenger>> getPassengersOnTheFloor() {
        return passengersOnTheFloor;
    }

    public void printMovedPassengersOnTheFloor() {
        while(elevatorThread.isAlive())
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        getPassengersOnTheFloor().values().stream().forEach(list -> list.stream().forEach(passenger -> System.out.println(passenger)));
    }
}
