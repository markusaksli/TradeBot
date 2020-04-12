import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentHandler {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static void addTask(Runnable task) {
        executorService.submit(task);
    }
}
