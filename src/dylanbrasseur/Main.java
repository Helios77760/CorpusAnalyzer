package dylanbrasseur;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

class ValueOcc
{
    public int value;
    public long occurences;
}

class Mots extends ValueOcc
{
    Mots(int value, long occurences) {
        this.value = value;
        this.occurences = occurences;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Mots && ((Mots) obj).value == value;
    }

    @Override
    public String toString()
    {
        return Main.getAccented(value);
    }
}

class Entry extends ValueOcc
{
    public ArrayList<Mots> follow;
    public int accented;

    Entry(int value, int accented, long occurences) {
        this.value = value;
        this.occurences = occurences;
        this.accented = accented;
        follow = new ArrayList<>();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Entry && ((Entry) obj).value == value;
    }

    @Override
    public String toString() {
        return Main.getWord(value) + "\t" + Main.getAccented(accented)+ "\t" + occurences + "\t" + followToString();
    }

    public String followToString()
    {
        StringBuilder sb = new StringBuilder();
        ArrayList<Mots> arr = new ArrayList<>(follow);
        arr.sort((m,n)->{
            if(m.occurences > n.occurences)
                return -1;
            else if(m.occurences == n.occurences)
                return 0;
            return 1;
        });
        for(int i=0; i<Main.maxPropositions && i<arr.size();i++)
        {
            sb.append(arr.get(i)).append("\t");
        }
        return sb.toString();
    }
}

public class Main {

    private static final ArrayList<String> dictionary = new ArrayList<>();
    private static final ArrayList<String> accentDictionnary = new ArrayList<>();
    private static final Vector<String> printQueue = new Vector<>();
    private static ArrayList<Entry> wordAndFollow;
    private static boolean folderInputMode = false;
    private static boolean generatesOwnDictionnary = false;
    private static boolean advanced = false;
    private static String path="";
    private static final String dictionaryName="dictionary.txt";
    public static final int maxPropositions = 10;
    private static long numberOfFiles=0;
    private static long fileSize=0;
    private static long numberOfFailures=0;
    private static long endTime;
    private static long launchTime;
    private static boolean useNewDictionnary=false;

    public static void main(String[] args) {

        wordAndFollow = new ArrayList<>();
        PrintEngine print = new PrintEngine();
        print.start();
        printWelcome();
        launchTime = System.currentTimeMillis();
        if(args.length < 1)
        {
            System.err.println("Not enough arguments");
            return;
        }

        for(String str : args)
        {
            if(str.length() > 0)
            {
                if(str.charAt(0) == '-')
                {
                    if(str.length() > 1)
                    {
                        if(str.charAt(1) == 'f')
                        {
                            folderInputMode = false;
                        }else if(str.charAt(1) == 'r')
                        {
                            folderInputMode=true;
                        }else if(str.charAt(1) == 'd')
                        {
                            generatesOwnDictionnary = true;
                        }else if(str.charAt(1) == 'a')
                        {
                            advanced = true;
                        }else if(str.charAt(1) == 'n')
                        {
                            useNewDictionnary=true;
                        }
                    }
                }else
                {
                    path = str;
                }
            }
        }

        if(Objects.equals(path, ""))
        {
            System.err.println("No path");
            return;
        }



        wordAndFollow.add(new Entry(0, 0, 0));
        dictionary.add("");
        accentDictionnary.add("");
        boolean dictionaryLoaded;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(useNewDictionnary ? new FileInputStream(dictionaryName) : Main.class.getClassLoader().getResourceAsStream(dictionaryName)))){
            addPrint("Using " + (useNewDictionnary ? "external" : "internal") + " dictionary...\n");
            String line;
            Integer frequencie;
            int index=1;
            while((line = br.readLine() )!= null)
            {
                String[] values = line.split("\t");
                try
                {
                    frequencie = Integer.parseInt(values[2]);
                    if(frequencie<=0)
                        frequencie=1;
                }catch(NumberFormatException e)
                {
                    frequencie=1;
                }
                dictionary.add(values[0]);
                accentDictionnary.add(values[1]);
                Entry entry = new Entry(index, index, frequencie);
                index++;
                wordAndFollow.add(entry);
            }
            dictionaryLoaded=index > 1;
        } catch(IOException e) {
            e.printStackTrace();
            dictionaryLoaded=false;
        }
        if(dictionaryLoaded)
        {
            if(generatesOwnDictionnary)
            {
                resetStats();
                generateDictionary();
                endTime = System.currentTimeMillis();
                printStats();
                saveDictionary();

            }
            if(advanced)
            {
                resetStats();
                addPrint("\n----------------\n");
                addPrint("Processing data\n");
                addPrint("----------------\n\n");
                if(folderInputMode)
                {
                    try {
                        processFolder(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else
                {
                    try {
                        processFile(new File(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                endTime = System.currentTimeMillis();
                printStats();
                addPrint("\nWriting results... ");
                try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dictionnaryFinal.txt"))))
                {
                    int emptyEntry = firstEntry("", wordAndFollow);
                    if(emptyEntry != -1)
                    {
                        Entry entry = wordAndFollow.get(emptyEntry);
                        bw.write(entry.followToString());
                        wordAndFollow.remove(emptyEntry);
                    }
                    for (Entry anArr : wordAndFollow) {
                        bw.newLine();
                        bw.write(anArr.toString());
                    }
                    addPrint("Done\n");

                } catch (IOException e) {
                    addPrint("Failed\n");
                    e.printStackTrace();
                }
            }
        }else
        {
            addPrint("No dictionnary...");
        }



        try {
            print.termination();
            synchronized (printQueue)
            {
                printQueue.notifyAll();
            }
            print.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static void resetStats()
    {
        numberOfFiles=0;
        fileSize=0;
        numberOfFailures=0;
        endTime=System.currentTimeMillis();
        launchTime=System.currentTimeMillis();
    }

    private static void saveDictionary() {
        addPrint("\nWriting results... ");
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dictionaryName))))
        {
            long size = wordAndFollow.size();
            for (int i = 0; i < size; i++) {
                Entry word = wordAndFollow.get(i);
                bw.write(dictionary.get(word.value)+ "\t" + accentDictionnary.get(word.value) + "\t" + word.occurences);
                if(i<size-1)
                    bw.newLine();
            }
            addPrint("Done\n\n");

        } catch (IOException e) {
            addPrint("Failed\n\n");
            e.printStackTrace();
        }
    }

    private static void generateDictionary() {
        addPrint("\n----------------\n");
        addPrint("Generating dictionnary from files...\n");
        addPrint("----------------\n\n");
        for(Entry e : wordAndFollow)
        {
            e.occurences*=10000;
        }
        if(folderInputMode)
        {
            try {
                dictionaryProcessFolder(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else
        {
            try {
                dictionaryProcessFile(new File(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        addPrint("\nFile Processing done, cleaning up the dictionnary... \n");
        for(int i=0;i<wordAndFollow.size();)
        {
            if(wordAndFollow.get(i).occurences < 10000)
            {
                dictionary.remove(i);
                accentDictionnary.remove(i);
                wordAndFollow.remove(i);
            }else
            {
                wordAndFollow.get(i).value=i;
                wordAndFollow.get(i).occurences/=10000;
                i++;
            }
        }
    }

    private static void dictionaryProcessFile(File file) throws IOException
    {
        numberOfFiles++;
        if(file != null && file.exists() && !file.isDirectory())
        {

            fileSize+=file.length();
            addPrint(String.format("%-100s", "Processing "+file.getName()+"... "));
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while((line = br.readLine())!= null)
            {
                String[] words = line.split("[-*.?!:;, \t\"'«»’–)(]");
                for(String word : words)
                {
                    String accented = word;
                    word = sanitize(word);
                    if(word.length() > 1)
                    {
                        int index = insertPosDictionary(word);
                        if(index < dictionary.size() && dictionary.get(index).equals(word))
                        {
                            wordAndFollow.get(index).occurences++;
                        }else
                        {
                            dictionary.add(index, word);
                            accentDictionnary.add(index, accented);
                            wordAndFollow.add(index, new Entry(index, index,1));
                        }
                    }
                }
            }
            addPrint("OK\n");
        }
    }

    private static void dictionaryProcessFolder(String folderPath) throws IOException
    {
        File folder = new File(folderPath);

        if(folder.exists())
        {
            File[] files = folder.listFiles(pathname -> {
                String name = pathname.getName().toLowerCase();
                return name.endsWith(".txt") && pathname.isFile();
            });
            if(files != null && files.length > 0)
            {
                String format = "[%0"+((int)(Math.floor(Math.log10(files.length))+1))+"d/"+files.length+"] ";
                for(File file : files)
                {
                    try
                    {
                        addPrint(String.format(format, numberOfFiles+1));
                        dictionaryProcessFile(file);

                    }catch(IOException e)
                    {
                        numberOfFailures++;
                        addPrint("Failed\n");
                    }
                }
            }
        }else
            throw new FileNotFoundException("Folder doesn't exist");


    }

    private static void printStats() {
        addPrint("----------------\n");
        addPrint("Processed "+ numberOfFiles + " files in "+((endTime-launchTime)/60000)+"min "+(((endTime-launchTime)/1000)%60)+"s\n");
        addPrint("Size of all files : "+ (fileSize>1000000 ? fileSize/1000000 + "MB" : fileSize/1000 + "KB")+"\n");
        addPrint("Number of failures : " + numberOfFailures + String.format(" (%.2f%%)\n", ((double)numberOfFailures)/numberOfFiles));
        long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        addPrint("Using : "+((memory > 1000000) ? memory/1000000 + "M" : memory/1000 + "K") + "B of memory\n");
        addPrint("----------------\n");
    }

    private static void printWelcome()
    {
        addPrint("----------------\n");
        addPrint("CORPUS ANALYZER\n");
        addPrint("----------------\n");
    }

    private static int insertPosDictionary(String word)
    {
        if(dictionary.size() == 0)
            return 0;
        int result = 0;
        int size = dictionary.size();
        int min=0, max = size;
        while(min <= max)
        {
            int mid = min+((max-min)/2);
            if(mid>=size)
                return size;
            String wordMid = dictionary.get(mid);
            int value = wordMid.compareTo(word);
            if(value == 0)
            {
                result=mid;
                max=mid-1;
            }else if(value > 0)
            {
                max = mid-1;
            }else
            {
                min = mid+1;
                result = min;
            }
        }
        return result;
    }

    private static void processFile(File file) throws IOException
    {
        numberOfFiles++;
        if(file != null && file.exists() && !file.isDirectory())
        {
            fileSize+=file.length();
            addPrint(String.format("%-100s", "Processing "+file.getName()+"... "));
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line, related;
            while((line = br.readLine())!= null)
            {
                String[] sentences = line.split("[.?!:;]");
                for(String sentence : sentences)
                {
                    related="";
                    String[] words = sentence.split("[-, \t\"'«»’–)(]");
                    for(String word : words)
                    {
                        String accentedWord = word.toUpperCase().replaceAll("[^\\p{IsAlphabetic}]", "");
                        word = sanitize(word);
                        if(addWord(word, accentedWord, related))
                        {
                            related=word;
                        }
                    }
                }
            }
            addPrint("OK\n");
        }
    }

    private static void processFolder(String folderPath) throws IOException
    {
        File folder = new File(folderPath);
        if(folder.exists())
        {
            File[] files = folder.listFiles(pathname -> {
                String name = pathname.getName().toLowerCase();
                return name.endsWith(".txt") && pathname.isFile();
            });
            if(files != null && files.length > 0)
            {
                String format = "[%0"+((int)(Math.floor(Math.log10(files.length))+1))+"d/"+files.length+"] ";
                for(File file : files)
                {
                    try
                    {
                        addPrint(String.format(format, numberOfFiles+1));
                        processFile(file);

                    }catch(IOException e)
                    {
                        numberOfFailures++;
                        addPrint("Failed\n");
                    }
                }
            }
        }else
            throw new FileNotFoundException("Folder doesn't exist");
    }

    private static String sanitize(String word)
    {
        word = Normalizer.normalize(word, Normalizer.Form.NFD);
        word = word.replaceAll("[\\u0300-\\u036F]", "");
        word = word.toUpperCase();
        return word.replaceAll("[^A-Z]+", "");
    }

    private static boolean addWord(String word, String accented, String related)
    {
        int relatedPos = firstEntry(related, wordAndFollow);
        int wordPos = firstEntry(word, wordAndFollow);
        if(word.length()>1 && relatedPos!=-1 && wordPos!=-1) //In the dictionnary
        {
            Entry entry = wordAndFollow.get(relatedPos);
            int insertPosition = insertPos(accented, entry.follow);
            int size = entry.follow.size();
            if(size > 0 && insertPosition<size && entry.follow.get(insertPosition).value == wordPos)
            {
                entry.follow.get(insertPosition).occurences++;
            }else
            {
                entry.follow.add(insertPosition, new Mots(wordPos, 1));
            }
            return true;
        }
        return false;
    }
    private static void addPrint(String str)
    {
        synchronized (printQueue)
        {
            printQueue.add(str);
            printQueue.notifyAll();
        }

    }

    private static int firstEntry(String word, ArrayList<Entry> list)
    {
        int result = -1;
        int size = list.size();
        int min=0, max = size;

        while(min <= max)
        {
            int mid = min+(max-min)/2;
            if(mid >= size)
                return -1;
            String wordMid = dictionary.get(list.get(mid).value);
            int value = wordMid.compareTo(word);
            if(value == 0)
            {
                result=mid;
                max=mid-1;
            }else if(value > 0)
            {
                max = mid-1;
            }else
            {
                min = mid+1;
            }
        }
        return result;
    }

    private static int insertPos(String word, ArrayList<Mots> list)
    {
        if(list.size() == 0)
            return 0;
        int result = 0;
        int size = list.size();
        int min=0, max = size;
        while(min <= max)
        {
            int mid = min+((max-min)/2);
            if(mid>=size)
                return size;
            String wordMid = dictionary.get(list.get(mid).value);
            int value = wordMid.compareTo(word);
            if(value == 0)
            {
                result=mid;
                max=mid-1;
            }else if(value > 0)
            {
                max = mid-1;
            }else
            {
                min = mid+1;
                result = min;
            }
        }
        return result;
    }

    public static String getWord(int i)
    {
        return dictionary.get(i);
    }

    public static String getAccented(int i)
    {
        return accentDictionnary.get(i);
    }

    public static class PrintEngine extends Thread {

        boolean terminate = false;
        @Override
        public void run() {
            String str="";
            boolean print=false;
            while(!terminate || !printQueue.isEmpty()) {
                synchronized (printQueue) {
                    if(printQueue.isEmpty())
                    {
                        try {
                            printQueue.wait();
                        } catch (InterruptedException ignored) {

                        }
                    }
                    if(!printQueue.isEmpty())
                    {
                        str = printQueue.remove(0);
                        print=true;
                    }

                }
                if(print)
                    System.out.print(str);
            }
        }

        void termination()
        {
            terminate=true;
        }
    }
}
