import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;


public class Main {

    // ---------- helper classes ----------

    static class Room {
        private final String roomNo;
        private boolean occupied;
        private String studentId = "";
        private String studentName = "";

        Room(String roomNo) { this.roomNo = roomNo; }

        String getRoomNo()      { return roomNo; }
        boolean isOccupied()    { return occupied; }
        String getStudentId()   { return studentId; }
        String getStudentName() { return studentName; }

        void assign(String id, String name) { occupied = true; studentId = id; studentName = name; }
        void vacate()                       { occupied = false; studentId = ""; studentName = ""; }
    }

    static class Request implements Comparable<Request> {
        private final String studentId;
        private final String name;
        private final int urgency;      // 1 = urgent, 2 = normal
        private final int requestNo;    // grows by 1 for every request (works as request time)

        Request(String studentId, String name, int urgency, int requestNo) {
            this.studentId = studentId;
            this.name = name;
            this.urgency = urgency;
            this.requestNo = requestNo;
        }

        String getStudentId() { return studentId; }
        String getName()      { return name; }
        int getUrgency()      { return urgency; }

        /** Urgency first, then request time. */
        @Override
        public int compareTo(Request o) {
            if (urgency != o.urgency) return Integer.compare(urgency, o.urgency);
            return Integer.compare(requestNo, o.requestNo);
        }
    }

    /** One node of the linked list of room assignments. */
    static class AssignmentNode {
        private final String studentId;
        private final String roomNo;
        private AssignmentNode next;

        AssignmentNode(String studentId, String roomNo) {
            this.studentId = studentId;
            this.roomNo = roomNo;
        }

        String getStudentId()       { return studentId; }
        String getRoomNo()          { return roomNo; }
        AssignmentNode getNext()    { return next; }
        void setNext(AssignmentNode n) { next = n; }
    }

    // ---------- data ----------

    static final String FILE_NAME = "hostel_rooms.txt";

    static Room[] rooms = { new Room("H101"), new Room("H102") };
    static HashMap<String, String> studentRoom = new HashMap<>();     // studentId -> roomNo
    static PriorityQueue<Request> requests = new PriorityQueue<>();
    static AssignmentNode head = null;                                // linked list of assignments
    static AssignmentNode tail = null;
    static int requestCounter = 0;

    static Scanner sc = new Scanner(System.in);

    // ---------- linked list ----------

    static void listAdd(String studentId, String roomNo) {
        AssignmentNode node = new AssignmentNode(studentId, roomNo);
        if (head == null) { head = node; tail = node; }
        else { tail.setNext(node); tail = node; }
    }

    static void listRemove(String roomNo) {
        AssignmentNode prev = null, cur = head;
        while (cur != null && !cur.getRoomNo().equals(roomNo)) { prev = cur; cur = cur.getNext(); }
        if (cur == null) return;
        if (prev == null) head = cur.getNext(); else prev.setNext(cur.getNext());
        if (cur == tail) tail = prev;
    }

    // ---------- helpers ----------

    static Room findFreeRoom() {
        for (Room r : rooms) if (!r.isOccupied()) return r;          // never returns an occupied room
        return null;
    }

    static Room findRoom(String roomNo) {
        for (Room r : rooms) if (r.getRoomNo().equalsIgnoreCase(roomNo)) return r;
        return null;
    }

    static Request findPending(String studentId) {
        for (Request r : requests) if (r.getStudentId().equalsIgnoreCase(studentId)) return r;
        return null;
    }

    static String ask(String prompt) {
        System.out.print(prompt);
        return sc.hasNextLine() ? sc.nextLine().trim() : null;
    }

    static void assignRoom(Room room, String id, String name) {
        room.assign(id, name);
        studentRoom.put(id, room.getRoomNo());
        listAdd(id, room.getRoomNo());
    }

    // ---------- file handling ----------

    static void loadData() {
        File f = new File(FILE_NAME);
        if (!f.exists()) return;                                     // first run: both rooms free
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",", -1);
                if (p[0].equals("REQUEST") && p.length == 4) {
                    try {
                        int u = Integer.parseInt(p[3].trim());
                        if (u == 1 || u == 2) requests.add(new Request(p[1].trim(), p[2].trim(), u, requestCounter++));
                    } catch (NumberFormatException e) { /* ignore a wrong line */ }
                } else {
                    Room r = findRoom(p[0].trim());
                    if (r != null && p.length >= 4 && p[1].trim().equals("OCCUPIED")
                            && !studentRoom.containsKey(p[2].trim())) {
                        assignRoom(r, p[2].trim(), p[3].trim());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + FILE_NAME + ": " + e.getMessage());
        }
    }

    static void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Room r : rooms) {
                if (r.isOccupied()) pw.println(r.getRoomNo() + ",OCCUPIED," + r.getStudentId() + "," + r.getStudentName());
                else pw.println(r.getRoomNo() + ",FREE");
            }
            PriorityQueue<Request> copy = new PriorityQueue<>(requests);   // keep the priority order
            while (!copy.isEmpty()) {
                Request q = copy.poll();
                pw.println("REQUEST," + q.getStudentId() + "," + q.getName() + "," + q.getUrgency());
            }
        } catch (IOException e) {
            System.out.println("Could not save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    // ---------- features ----------

    static void requestRoom() {
        String id = ask("Enter student ID: ");
        if (id == null) return;
        if (id.isEmpty()) { System.out.println("Student ID cannot be empty."); return; }
        if (studentRoom.containsKey(id)) {                           // one student, one room
            System.out.println("Student " + id + " already has room " + studentRoom.get(id) + ".");
            return;
        }
        if (findPending(id) != null) {
            System.out.println("Student " + id + " already has a request waiting.");
            return;
        }
        String name = ask("Enter student name: ");
        if (name == null) return;
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }
        String u = ask("Urgency (1 = urgent, 2 = normal): ");
        if (u == null) return;
        if (!u.equals("1") && !u.equals("2")) { System.out.println("Invalid urgency. Enter 1 or 2."); return; }

        requests.add(new Request(id, name, Integer.parseInt(u), requestCounter++));
        if (findFreeRoom() != null) System.out.println("Request recorded for " + name + ".");
        else System.out.println("No room available. Request added to priority queue.");
    }

    /** Greedy: while a room is free, give it to the best waiting request. */
    static void allocateRooms() {
        if (requests.isEmpty()) { System.out.println("No requests waiting."); return; }
        if (findFreeRoom() == null) { System.out.println("No room available."); return; }

        while (findFreeRoom() != null && !requests.isEmpty()) {
            Request q = requests.poll();                             // highest priority request
            if (studentRoom.containsKey(q.getStudentId())) continue; // already has a room, skip
            Room room = findFreeRoom();
            assignRoom(room, q.getStudentId(), q.getName());
            if (q.getUrgency() == 1) System.out.println("Room assigned to " + q.getName() + " (urgent request served first).");
            else System.out.println("Room assigned to " + q.getName() + ".");
            System.out.println("Room: " + room.getRoomNo());
        }
    }

    static void searchStudent() {
        String id = ask("Enter student ID: ");
        if (id == null) return;
        String roomNo = studentRoom.get(id);                         // HashMap lookup
        if (roomNo != null) {
            Room r = findRoom(roomNo);
            System.out.println("Student " + id + " (" + r.getStudentName() + ") is in room " + roomNo + ".");
            return;
        }
        Request q = findPending(id);
        if (q != null) System.out.println("Student " + id + " (" + q.getName() + ") has a request waiting.");
        else System.out.println("Student " + id + " not found.");
    }

    static void vacateRoom() {
        String no = ask("Enter room number: ");
        if (no == null) return;
        Room r = findRoom(no);
        if (r == null) { System.out.println("Invalid room number."); return; }
        if (!r.isOccupied()) { System.out.println("Room " + r.getRoomNo() + " is already free."); return; }

        String id = r.getStudentId();
        studentRoom.remove(id);
        listRemove(r.getRoomNo());
        r.vacate();
        System.out.println("Room " + r.getRoomNo() + " vacated (student " + id + ").");
    }

    static void displayStatus() {
        System.out.println("----- Room status -----");
        for (Room r : rooms) {
            if (r.isOccupied()) System.out.println(r.getRoomNo() + " | OCCUPIED | " + r.getStudentId() + " (" + r.getStudentName() + ")");
            else System.out.println(r.getRoomNo() + " | FREE");
        }
        System.out.println("Pending requests: " + requests.size());
        PriorityQueue<Request> copy = new PriorityQueue<>(requests);
        int i = 1;
        while (!copy.isEmpty()) {
            Request q = copy.poll();
            System.out.println("  " + (i++) + ". " + q.getStudentId() + " (" + q.getName() + ") - "
                    + (q.getUrgency() == 1 ? "urgent" : "normal"));
        }
    }

    // ---------- main ----------

    static void showMenu() {
        System.out.println();
        System.out.println("===== HOSTEL ROOM ALLOCATION SYSTEM =====");
        System.out.println("1. Request room");
        System.out.println("2. Allocate rooms");
        System.out.println("3. Search student");
        System.out.println("4. Vacate room");
        System.out.println("5. Display status");
        System.out.println("6. Exit");
        System.out.print("Enter your choice: ");
    }

    public static void main(String[] args) {
        loadData();                                                  // load data at start

        while (true) {
            showMenu();
            if (!sc.hasNextLine()) break;                            // input finished
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1": requestRoom();   break;
                case "2": allocateRooms(); break;
                case "3": searchStudent(); break;
                case "4": vacateRoom();    break;
                case "5": displayStatus(); break;
                case "6":
                    saveData();                                      // save data on exit
                    System.out.println("Data saved. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Enter 1 to 6.");
            }
        }
        saveData();
    }
}
