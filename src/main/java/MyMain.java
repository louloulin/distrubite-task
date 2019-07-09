import com.lin.task.*;

public class MyMain {
    public static void main(String[] args) throws InterruptedException {
        WorkSource workSource = new WorkSource("1", "work1", new byte[10]);
        BootStrap.startBackend(2551, "backend");
        Thread.sleep(5000);
        BootStrap.startBackend(2552, "backend");
        BootStrap.startWorker(0,new WorkProcess());
        BootStrap.startWorker(0,new WorkProcess());
        BootStrap.startWorker(0,new WorkProcess());
        BootStrap.startWorker(0,new WorkProcess());
        BootStrap.startWorker(0,new WorkProcess());
        BootStrap.startWorker(0,new WorkProcess());
//        BootStrap.startWorker(6667,new WorkProcess());
//        BootStrap.startWorker(6668,new WorkProcess());
//        BootStrap.startWorker(6669,new WorkProcess());
//        BootStrap.startWorker(6670,new WorkProcess());

        Thread.sleep(5000);
        BootStrap.startFrontend(0, DefaultWorkProducer.class, WorkResultConsumer.class,new DefaultResultHandler());
    }
}
