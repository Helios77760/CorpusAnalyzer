package dylanbrasseur;

import java.io.*;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Objects;
import java.util.Vector;

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
}

public class Main {
    private static PrintThread print;
    private static HashMap<String, Entry> wordAndFollow;
    private static boolean folderInputMode = false;
    private static String path="";
    private static final String dictionaryName="dictionary.txt";

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
        print = new PrintThread();

        try(BufferedReader br = new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream(dictionaryName)))){
            String line;
            Integer frequencie;
            while((line = br.readLine() )!= null)
            {
                String[] values = line.split(" ");
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
            print.addString("Processing "+file.getName()+"... ");
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while((line = br.readLine())!= null)
            {
                line = prepareLine(line);
                String[] sentences = line.split("[.?!:;]");
                for(String sentence : sentences)
                {
                    String[] words = sentence.split("[-, \"']");
                    //TODO
                }
            }
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
                        print.addString("OK\n");
                    }catch(IOException e)
                    {
                        print.addString("Failed\n");
                    }
                }
            }
        }else
            throw new FileNotFoundException("Folder doesn't exist");
    }

    public static class PrintThread extends Thread {
        private Vector<String> printQueue;
        PrintThread()
        {
            printQueue = new Vector<>();
        }
        @Override
        public void run() {
            while(!printQueue.isEmpty())
            {
                System.out.print(printQueue.remove(0));
            }
        }

        void addString(String str)
        {
            printQueue.add(str);
            if(!this.isAlive())
                this.start();
        }
    }
}
