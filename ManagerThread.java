//Branoiu Mihai-Catalin 335CA

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import static java.lang.Math.min;

public class ManagerThread extends Thread {
    private final int id;
    private int start;
    private int end;
    public static final int MAX_NUM_WORKERS = 5;

    public ManagerThread(int id) {
        this.id = id;
    }

    public void run() {


        //deschid file descriptor pentru citirea fisierului de comenzi
        RandomAccessFile ordersFile;
        long orderFileSize;
        try {
            ordersFile = new RandomAccessFile(Tema2.ordersFilePath, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        //calculez dimensiunea in bytes a fisierului de intrare
        try {
            orderFileSize = ordersFile.length();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        calcinterval(orderFileSize);

        //daca pointerul nu este la prima linie (ca in cazul threadului 0) aduc pointerul de citire la prima linie
        if (start > 0) {
            try {
                ordersFile.seek(start - 1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //citesc in "gol" pana la urmatoarea linie noua
            try {
                String x = ordersFile.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        //citesc linie cu linie pana cand pointerul de citire depaseste limita dreapta (end)
        while (true) {
            try {
                if (!(ordersFile.getFilePointer() < end)) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                //citesc linia
                String line = ordersFile.readLine();
                //apelez functia de livrare a comenzii
                shipOrder(line);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void calcinterval(long orderFileSize) {
        //calculez rangeul de bytes pentru citire paralela
        start = (int) (this.id * (double) orderFileSize / Tema2.procNum);
        end = (int) (min((this.id + 1) * (double) orderFileSize / Tema2.procNum, orderFileSize));
    }
    public void shipOrder(String line) {
        //parsare linie comanda
        List<String> argList = Arrays.asList(line.split(","));
        String orderNo = argList.get(0);
        int productsNo = Integer.parseInt(argList.get(1));

        //daca comanda nu are produse nu mai cautam produse pentru ea
        if(productsNo == 0)
            return;

        //creez semafor pentru asteptarea threadurilor de nivel 2
        Semaphore semaphore = new Semaphore(-productsNo+1);

        //atribui un numar fix de threaduri de layer 2 pentru fiecare comanda si incerc sa o execut cu workerii pe care
        //ii am disponibili in pool
        for(int i=0; i< MAX_NUM_WORKERS; i++){
            Tema2.tpe.execute(new WorkerThread(i, orderNo, semaphore));
        }

        //astpet eliberarea semaforului pentru a ma asigura ca threadurile de nivel 2 au terminat executia
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //scriu in writer ca am expediat comanda
        try {
            Tema2.ordersBuffer.write(line + ",shipped\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
