/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.moneylaundering;

import static edu.eci.arsw.moneylaundering.MoneyLaundering.NUMTHREAD;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author juan.garcia-ga
 */
public class MoneyLaunderingThread extends Thread{
    private TransactionAnalyzer transactionAnalyzer;
    private TransactionReader transactionReader;
    private int amountOfFilesTotal;
    private int numberThread;
    private AtomicInteger amountOfFilesProcessed;
    private List<File> transactionFiles;
    
    public MoneyLaunderingThread(TransactionAnalyzer transactionAnalyzer, TransactionReader transactionReader, AtomicInteger amountOfFilesProcessed, int numberThread, List<File> transactionFiles) {
        this.transactionAnalyzer = transactionAnalyzer;
        this.transactionReader = transactionReader;
        this.amountOfFilesProcessed = amountOfFilesProcessed;
        this.numberThread = numberThread;
        this.transactionFiles = transactionFiles;
    }
    
    
    
    
    public void start(){
        amountOfFilesProcessed.set(0);              
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

}
