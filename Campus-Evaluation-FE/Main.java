import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

class Notification implements Comparable<Notification> {
    private final String id;
    private final String content;
    private final int importanceScore;
    private final long timestamp;

    public Notification(String id, String content, int importanceScore) {
        this.id = id;
        this.content = content;
        this.importanceScore = importanceScore;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public int getImportanceScore() { return importanceScore; }

    @Override
    public int compareTo(Notification other) {
        return Integer.compare(this.importanceScore, other.importanceScore);
    }

    @Override
    public String toString() {
        return "[ID: " + id + " | Score: " + importanceScore + "] " + content;
    }
}

class NotificationInboxManager {
    private static final Logger logger = Logger.getLogger(NotificationInboxManager.class.getName());
    private final int capacity;
    private final PriorityQueue<Notification> minHeap;

    public NotificationInboxManager(int capacity) {
        this.capacity = capacity;
        this.minHeap = new PriorityQueue<>(capacity);
    }


    private boolean loggingMiddleware(Notification notification) {
        logger.log(Level.INFO, "[Middleware Log] Intercepted Notification ID: {0}, Score: {1}",
                new Object[]{notification.getId(), notification.getImportanceScore()});
        
        if (notification.getImportanceScore() < 0) {
            logger.log(Level.WARNING, "Rejected structural anomaly: Negative importance score on ID: {0}", notification.getId());
            return false;
        }
        return true;
    }


    public synchronized void processIncomingNotification(Notification notification) {
        if (!loggingMiddleware(notification)) {
            return;
        }

        if (minHeap.size() < capacity) {
            minHeap.offer(notification);
            logger.log(Level.INFO, "Buffered notification into inbox. Current count: {0}", minHeap.size());
        } else {
            Notification lowestCurrentTop = minHeap.peek();
            if (lowestCurrentTop != null && notification.getImportanceScore() > lowestCurrentTop.getImportanceScore()) {
                Notification removed = minHeap.poll();
                minHeap.offer(notification);
                logger.log(Level.INFO, "Evicted lower priority element ID: {0} to make room for ID: {1}",
                        new Object[]{removed.getId(), notification.getId()});
            } else {
                logger.log(Level.INFO, "Notification ID: {0} filtered out (below top-10 threshold).", notification.getId());
            }
        }
    }
    public List<Notification> getTopNotifications() {
        List<Notification> sortedView = new ArrayList<>(minHeap);
        sortedView.sort(Collections.reverseOrder());
        return sortedView;
    }
}

public class Main {
    public static void main(String[] args) {
        NotificationInboxManager inbox = new NotificationInboxManager(10);

        for (int i = 1; i <= 12; i++) {
            int simulatedScore = (i * 13) % 100; 
            Notification notif = new Notification("REQ-" + i, "Campus Event Payload Details " + i, simulatedScore);
            inbox.processIncomingNotification(notif);
        }

        System.out.println("\n--- Current Top 10 Inbox Display (Highest Importance First) ---");
        int rank = 1;
        for (Notification n : inbox.getTopNotifications()) {
            System.out.println("Rank " + (rank++) + ": " + n);
        }
    }
}