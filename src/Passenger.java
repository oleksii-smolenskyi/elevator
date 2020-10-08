public class Passenger implements Runnable {
    private static int COUNTER = 0;

    private final int id;
    // поверх призначення
    private int floorTo;
    // поточний поверх
    private int currentFloor;
    private Building whereIAm;

    // статус пасажира
    enum State {
        WANT_TO_MOVE, WAIT, IN_PROGRESS, MOVED;
    }

    private State state = State.WANT_TO_MOVE;

    public void setState(State state) {
        this.state = state;
    }

    public Passenger(int currentFloor, int floorTo) {
        COUNTER++;
        this.id = COUNTER;
        this.currentFloor = currentFloor;
        this.floorTo = floorTo;
    }

    // метод повідомлення пасажира, що можна попробувати увійти в ліфт
    public void comeIn(Elevator elevator) {
        if(elevator != null) {
            try {
                elevator.loadPassenger(this);
            } catch (ElevatorIsFullException e) {
                this.state = State.WANT_TO_MOVE;
            }
        }
    }

    public int getFloorTo() {
        return floorTo;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
        if(currentFloor == floorTo)
            state = State.MOVED;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    @Override
    public void run() {
        // поки пасажир не добрався спотрібного поверху
        while(!state.equals(State.MOVED)) {
            // якщо пасажиру потрібно скористатись ліфтом, він перевіряє статус кнопки виклику і слідкує за ліфтом
            if(state.equals(State.WANT_TO_MOVE)) {
                Elevator elevator = whereIAm.getElevator();
                if (!elevator.checkCallButton(currentFloor))
                    elevator.call(currentFloor, this);
                keepEyeOnElevator(elevator);
            }
            // wait
            try {
                Thread.sleep(Elevator.MSEC_PER_FLOOR);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // слідкувати за ліфтом
    private void keepEyeOnElevator(Elevator elevator) {
        if(elevator != null) {
            System.out.println(this + " спостерігає за ліфтом");
            elevator.addWatcher(this);
            state = State.WAIT;
        }
    }

    public void setWhereIAm(Building whereIAm) {
        this.whereIAm = whereIAm;
    }

    @Override
    public String toString() {
        return "Passenger{" +
                "id=" + id +
                ", currentFloor=" + currentFloor +
                ", floorTo=" + floorTo +
                ", whereIAm=" + whereIAm +
                ", state=" + state +
                '}';
    }
}
