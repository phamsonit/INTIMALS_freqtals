import java.lang.InterruptedException;

public class TimeOut implements Runnable {

    private boolean taskComplete = false;
    private long times;

    public void setTaskComplete(boolean value){
        this.taskComplete = value;
    }

    public void setTimes(long t){
        this.times = t;
    }

    public void run() {
        try {
            Thread.sleep(times);
            if (taskComplete == false) {
                System.out.println("Timed Out : "+times/(60*1000)+" minutes");

                //report

                System.exit(2);

            }
        }catch (Exception e){System.out.println(e);}
    }

}


