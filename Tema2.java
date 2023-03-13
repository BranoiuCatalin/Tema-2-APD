//Branoiu Mihai-Catalin 335CA

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tema2 {
    public static ExecutorService tpe;
    public static int procNum;
    private static String filePath;
    public static String ordersFilePath;
    public static String productsFilePath;
    private static FileWriter ordersWriter;
    public static BufferedWriter ordersBuffer;

    private static FileWriter productsWriter;
    public static BufferedWriter productsBuffer;

    public static void main(String[] args) throws IOException {
        //preiau argumentele din linia de comanda
        filePath = args[0];
        procNum = Integer.parseInt(args[1]);
        tpe = Executors.newFixedThreadPool(Tema2.procNum);

        //generez filepathurile pentru cele doua fisiere de intrare
        ordersFilePath = filePath + "/orders.txt";
        productsFilePath = filePath + "/order_products.txt";

        //creez writer pentru fisierul de iesire pentru comenzi
        ordersWriter = new FileWriter("orders_out.txt");
        ordersBuffer = new BufferedWriter(ordersWriter);

        //creez writer pentru fisierul de iesire pentru produse
        productsWriter = new FileWriter("order_products_out.txt");
        productsBuffer = new BufferedWriter(productsWriter);

        //pornesc un numar de procNum threaduri Manager (nivel 1)
        ManagerThread[] t = new ManagerThread[procNum];
        for (int i = 0; i < procNum; ++i) {
            t[i] = new ManagerThread(i);
            t[i].start();
        }

        //astept inchiderea threadurilor de nivel 1
        for (int i = 0; i < procNum; ++i) {
            try {
                t[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //inchid ThreadPollExecutor
        tpe.shutdown();

        //inchid buffered writers pentru a scrie in fisiere
        ordersBuffer.close();
        productsBuffer.close();
    }
}
