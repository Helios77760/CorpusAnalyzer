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


    public Mots(int value, long occurences) {
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
        return Main.getWord(value);
    }
}

class Entry extends ValueOcc
{
    public ArrayList<Mots> follow;

    public Entry(int value, long occurences) {
        this.value = value;
        this.occurences = occurences;
        follow = new ArrayList<>();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Entry && ((Entry) obj).value == value;
    }

    @Override
    public String toString() {
        return Main.getWord(value) + "\t" + occurences + "\t" + followToString();
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
    public static final ArrayList<String> dictionary = new ArrayList<>();
    private static final Vector<String> printQueue = new Vector<>();
    private static PrintEngine print;
    private static ArrayList<Entry> wordAndFollow;
    private static boolean folderInputMode = false;
    private static String path="";
    private static final String dictionaryName="dictionary.txt";
    public static final int maxPropositions = 4;

    public static void main(String[] args) {
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

        wordAndFollow = new ArrayList<>();
        print = new PrintEngine();
        print.start();

        wordAndFollow.add(new Entry(0, 0));
        dictionary.add("");
        try(BufferedReader br = new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream(dictionaryName)))){
            String line;
            Integer frequencie;
            int index=1;
            while((line = br.readLine() )!= null)
            {
                String[] values = line.split("\t");
                try
                {
                    frequencie = Integer.parseInt(values[1]);
                }catch(NumberFormatException e)
                {
                    frequencie=1;
                }
                dictionary.add(values[0]);
                Entry entry = new Entry(index++, frequencie);
                wordAndFollow.add(entry);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

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

        addPrint("\nWriting results... ");
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dictionnaryFinal.txt"))))
        {
            int emptyEntry = firstEntry("", wordAndFollow);
            if(emptyEntry != -1)
            {
                Entry entry = wordAndFollow.get(emptyEntry);
                bw.write(entry.followToString());
                bw.newLine();
                wordAndFollow.remove(emptyEntry);
            }
            for (Entry anArr : wordAndFollow) {
                bw.write(anArr.toString());
                bw.newLine();
            }
            addPrint("Done\n");

        } catch (IOException e) {
            addPrint("Failed\n");
            e.printStackTrace();
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

    public static String prepareLine(String line)
    {
        line = Normalizer.normalize(line, Normalizer.Form.NFD);
        line = line.replaceAll("[\\u0300-\\u036F]", "");
        line = line.toUpperCase();
        return line;
    }

    public static void processFile(File file) throws IOException
    {

        if(file != null && file.exists() && file.getName().contains(".txt"))
        {
            addPrint("Processing "+file.getName()+"... ");
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line, related;
            while((line = br.readLine())!= null)
            {
                line = prepareLine(line);
                String[] sentences = line.split("[.?!:;]");
                for(String sentence : sentences)
                {
                    related="";
                    String[] words = sentence.split("[-, \t\"'«»’–)(]");
                    for(String word : words)
                    {
                        if(addWord(word, related))
                        {
                            related=word;
                        }
                    }
                }
            }
            addPrint("OK\n");
        }
    }

    public static void processFolder(String folderPath) throws IOException
    {
        File folder = new File(folderPath);
        if(folder.exists())
        {
            File[] files = folder.listFiles();
            if(files != null)
            {
                for(File file : files)
                {
                    try
                    {
                        processFile(file);

                    }catch(IOException e)
                    {
                        addPrint("Failed\n");
                    }
                }
            }
        }else
            throw new FileNotFoundException("Folder doesn't exist");
    }

    public static String sanitize(String word)
    {
        return word.replaceAll("[^A-Z]+", "");
    }

    public static boolean addWord(String word, String related)
    {
        int relatedPos = firstEntry(related, wordAndFollow);
        int wordPos = firstEntry(word, wordAndFollow);
        if(word.length()>2 && relatedPos!=-1 && wordPos!=-1) //In the dictionnary
        {
            Entry entry = wordAndFollow.get(relatedPos);
            int insertPosition = insertPos(word, entry.follow);
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
    static void addPrint(String str)
    {
        synchronized (printQueue)
        {
            printQueue.add(str);
            printQueue.notifyAll();
        }

    }

    public static int firstEntry(String word, ArrayList<Entry> list)
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

    public static int insertPos(String word, ArrayList<Mots> list)
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

        public void termination()
        {
            terminate=true;
        }
    }
}
