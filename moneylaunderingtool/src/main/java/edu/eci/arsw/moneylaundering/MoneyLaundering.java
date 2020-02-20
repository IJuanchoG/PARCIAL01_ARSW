package edu.eci.arsw.moneylaundering;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoneyLaundering 
{
    private TransactionAnalyzer transactionAnalyzer;
    private TransactionReader transactionReader;
    private int amountOfFilesTotal;
    private static int infoHilo;
    private AtomicInteger amountOfFilesProcessed;
    public static final int NUMTHREAD = 5;
    public static ArrayList<Thread> hilos;
    public static Object sincronizador = new Object();
    
    public MoneyLaundering()
    {
        transactionAnalyzer = new TransactionAnalyzer();
        transactionReader = new TransactionReader();
        amountOfFilesProcessed = new AtomicInteger();
    }

    public TransactionAnalyzer getTransactionAnalyzer() {
        return transactionAnalyzer;
    }

    public void setTransactionAnalyzer(TransactionAnalyzer transactionAnalyzer) {
        this.transactionAnalyzer = transactionAnalyzer;
    }

    public TransactionReader getTransactionReader() {
        return transactionReader;
    }

    public void setTransactionReader(TransactionReader transactionReader) {
        this.transactionReader = transactionReader;
    }

    public int getAmountOfFilesTotal() {
        return amountOfFilesTotal;
    }

    public void setAmountOfFilesTotal(int amountOfFilesTotal) {
        this.amountOfFilesTotal = amountOfFilesTotal;
    }

    public AtomicInteger getAmountOfFilesProcessed() {
        return amountOfFilesProcessed;
    }

    public void setAmountOfFilesProcessed(AtomicInteger amountOfFilesProcessed) {
        this.amountOfFilesProcessed = amountOfFilesProcessed;
    }

    public void processTransactionData()
    {
        amountOfFilesProcessed.set(0);
        List<File> transactionFiles = getTransactionFileList();        
        amountOfFilesTotal = transactionFiles.size();
        for(File transactionFile : transactionFiles)
        {            
            List<Transaction> transactions = transactionReader.readTransactionsFromFile(transactionFile);
            for(Transaction transaction : transactions)
            {
                transactionAnalyzer.addTransaction(transaction);
            }
            amountOfFilesProcessed.incrementAndGet();
        }
    }
    
    public void processTransactionDataModify()
    {
        amountOfFilesProcessed.set(0);
        List<File> transactionFiles = getTransactionFileList();        
        amountOfFilesTotal = transactionFiles.size();
        int filesByThread = amountOfFilesTotal/NUMTHREAD;
        int hiloinfo = MoneyLaundering.getInfoHilo();
        
        transactionFiles = transactionFiles.subList(hiloinfo*filesByThread, (hiloinfo+1)*filesByThread);
        for(File transactionFile : transactionFiles)
        {            
            List<Transaction> transactions = transactionReader.readTransactionsFromFile(transactionFile);
            for(Transaction transaction : transactions)
            {
                transactionAnalyzer.addTransaction(transaction);
            }
            amountOfFilesProcessed.incrementAndGet();
        }
    }

    public synchronized static int getInfoHilo() {
        return infoHilo;
    }

    public synchronized static void setInfoHilo(int infoHilo) {
        MoneyLaundering.infoHilo = infoHilo;
    }
    
    public List<String> getOffendingAccounts()
    {
        return transactionAnalyzer.listOffendingAccounts();
    }

    private List<File> getTransactionFileList()
    {
        List<File> csvFiles = new ArrayList<>();
        try (Stream<Path> csvFilePaths = Files.walk(Paths.get("src/main/resources/")).filter(path -> path.getFileName().toString().endsWith(".csv"))) {
            csvFiles = csvFilePaths.map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvFiles;
    }
    public static void main(String[] args)
    {
        System.out.println(getBanner());
        System.out.println(getHelp());
        MoneyLaundering moneyLaundering = new MoneyLaundering();
        //Thread processingThread = new Thread(() -> moneyLaundering.processTransactionData());
        //processingThread.start();
        hilos = new ArrayList<>();
        
        for (int i=0; i<NUMTHREAD; i++){
            synchronized(sincronizador){
                
                MoneyLaundering.setInfoHilo(i);
                Thread hilo = new Thread(()-> moneyLaundering.processTransactionDataModify()); 
                //MoneyLaunderingThread(moneyLaundering.transactionAnalyzer, moneyLaundering.transactionReader, moneyLaundering.amountOfFilesProcessed, i, transactionFiles);
                hilo.start();
                hilos.add(hilo);
                
            }
            
        }
        while(true)
        {
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            if(line.contains("exit"))
            {
                System.exit(0);
            }

            String message = "Processed %d out of %d files.\nFound %d suspect accounts:\n%s";
            List<String> offendingAccounts = moneyLaundering.getOffendingAccounts();
            String suspectAccounts = offendingAccounts.stream().reduce("", (s1, s2)-> s1 + "\n"+s2);
            message = String.format(message, moneyLaundering.amountOfFilesProcessed.get(), moneyLaundering.amountOfFilesTotal, offendingAccounts.size(), suspectAccounts);
            System.out.println(message);
        }
    }

    private static String getBanner()
    {
        String banner = "\n";
        try {
            banner = String.join("\n", Files.readAllLines(Paths.get("src/main/resources/banner.ascii")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return banner;
    }
    
   
    private static String getHelp()
    {
        String help = "Type 'exit' to exit the program. Press 'Enter' to get a status update\n";
        return help;
    }
}