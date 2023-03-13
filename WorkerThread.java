//Branoiu Mihai-Catalin 335CA

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import static java.lang.Math.min;

public class WorkerThread extends Thread{
    private final int id;
    private final String searchedOrder;
    private final Semaphore semaphore;
    private int start;
    private int end;
    private RandomAccessFile productsFile;
    private long productFileSize;

    public WorkerThread(int id, String searchedOrder, Semaphore semaphore) {
        this.id = id;
        this.searchedOrder = searchedOrder;
        this.semaphore = semaphore;
    }

    public void run() {


        //deschid file descriptor pentru citirea fisierului de produse
        try {
            productsFile = new RandomAccessFile(Tema2.productsFilePath, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        //calculez dimensiunea in bytes a fisierului de intrare
        try {
            productFileSize = productsFile.length();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //calculez intervalul de citire
        calcinterval(productFileSize);

        //daca pointerul nu este la prima linie (ca in cazul threadului 0) aduc pointerul de citire la prima linie
        if (start > 0) {
            try {
                productsFile.seek(start - 1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //citesc in "gol" pana la urmatoarea linie noua
            try {
                String x = productsFile.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //citesc linie cu linie pana cand pointerul de citire depaseste limita dreapta (end)
        while (true) {
            try {
                if (!(productsFile.getFilePointer() < end)) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                //citesc linia
                String line = productsFile.readLine();
                //apelez functia de livrare a produsului
                shipItem(line);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

    public void calcinterval(long productFileSize) {
        //calculez rangeul de bytes pentru citire paralela
        start = (int) (this.id * (double) productFileSize / ManagerThread.MAX_NUM_WORKERS);
        end = (int) (min((this.id + 1) * (double) productFileSize / ManagerThread.MAX_NUM_WORKERS, productFileSize));
    }

    public void shipItem(String line) {
        //parsare linie comanda
        List<String> argList = Arrays.asList(line.split(","));
        String orderNo = argList.get(0);
        String productNo = argList.get(1);

        //verific daca produsul curent apartine comenzii in curs de efectuare
        if(orderNo.compareTo(searchedOrder) == 0) {
            try {
                //scriu produsul expediat
                Tema2.productsBuffer.write(line + ",shipped\n");
                //semanlez trimiterea unui produs prin incrementarea semaforului
                semaphore.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
