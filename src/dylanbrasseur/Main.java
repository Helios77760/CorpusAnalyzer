package dylanbrasseur;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;


class Mots
{
    public String value;
    public long occurences;

    public Mots(String value, long occurences) {
        this.value = value;
        this.occurences = occurences;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Mots && ((Mots) obj).value.equals(value);
    }

    @Override
    public String toString()
    {
        return value;
    }
}

class Entry
{
    public String value;
    public long occurences;
    public HashMap<String, Mots> follow;

    public Entry(String value, long occurences) {
        this.value = value;
        this.occurences = occurences;
        follow = new HashMap<>();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Entry && ((Entry) obj).value.equals(value);
    }

    @Override
    public String toString() {
        return value + "\t" + occurences + "\t" + followToString();
    }

    public String followToString()
    {
        StringBuilder sb = new StringBuilder();
        ArrayList<Mots> arr = new ArrayList<>();
        for(String key : follow.keySet())
        {
            arr.add(follow.get(key));
        }
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
    private static final Vector<String> printQueue = new Vector<>();
    private static PrintEngine print;
    private static HashMap<String, Entry> wordAndFollow;
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

        wordAndFollow = new HashMap<>();
        print = new PrintEngine();
        print.start();


        try(BufferedReader br = new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream(dictionaryName)))){
            String line;
            Integer frequencie;
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
                Entry entry = new Entry(values[0], frequencie);
                wordAndFollow.put(entry.value, entry);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        wordAndFollow.put("", new Entry("", 0));
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
            if(wordAndFollow.containsKey(""))
            {
                Entry entry = wordAndFollow.get("");
                bw.write(entry.followToString());
                bw.newLine();
                wordAndFollow.remove("");
            }

            ArrayList<Entry> arr = new ArrayList<>();
            for(String key : wordAndFollow.keySet())
            {
                arr.add(wordAndFollow.get(key));
            }
            arr.sort(Comparator.comparing(a -> a.value));
            for (Entry anArr : arr) {
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

    public static boolean addWord(String word, String related)
    {
        if(word.length()>2 && wordAndFollow.containsKey(related) && wordAndFollow.containsKey(word)) //In the dictionnary
        {
            Entry entry = wordAndFollow.get(related);
            if(entry.follow.containsKey(word))
            {
                entry.follow.get(word).occurences++;
            }else
            {
                entry.follow.put(word, new Mots(word, 1));
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
