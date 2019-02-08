package be.intimals.freqt.util;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.InterruptedException;

public class TimeOut implements Runnable {

    private boolean taskComplete = false;
    private long times;
    private FileWriter report;

    public void setTaskComplete(boolean value){
        this.taskComplete = value;
    }

    public void setTimes(long t){
        this.times = t;
    }

    public void setReport(FileWriter report){
        this.report = report;
    }

    public void closeReport(){
        try {
            this.report.close();
        }catch (Exception e){}
    }


    public void run() {
        try {
            Thread.sleep(times);
            if (taskComplete == false) {
                System.out.println("Timed Out : "+times/(60*1000)+" minutes");
                closeReport();
                System.exit(2);
            }
        }catch (Exception e){
            System.out.println("Timeout:" + e);
            e.printStackTrace();
        }
    }

}


