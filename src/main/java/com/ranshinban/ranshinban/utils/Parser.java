package com.ranshinban.ranshinban.utils;

import javafx.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Parser
{

    static public HashMap<String,String> parseFile(String filePath, Set<String> keys) throws  IOException
    {
        FileInputStream inputStream = new FileInputStream(filePath);
        return  streamToMap(inputStream.readAllBytes(),";",keys);
    }
    static public HashMap<String,String> parseFile(File file, Set<String> keys) throws  IOException
    {
        FileInputStream inputStream = new FileInputStream(file);
        return  streamToMap(inputStream.readAllBytes(),";",keys);
    }

    static private HashMap<String,String> streamToMap(byte[] inputStream, String regex, Set<String> keys)
    {
        HashMap<String,String> parsedTokens = new HashMap<>();
        String[] streamContents = new String(inputStream).split(regex,-1);

        for(String configWord : streamContents)
        {
            String[] configLine = configWord.replaceAll("\\s","").split("=",-1);
            if(keys.contains(configLine[0]))
            {
                parsedTokens.put(configLine[0], configLine[1]);
            }
        }
        return parsedTokens;
    }
    static private List<Pair<String,String>> streamToList(byte[] inputStream, String regex, Set<String> keys)
    {
        ArrayList<Pair<String,String>> parsedTokens = new ArrayList<>();

        String[] streamContents = new String(inputStream).split(regex,-1);

        Iterator iterator = Arrays.stream(streamContents).iterator();

        while(iterator.hasNext())
        {
            String word = (String) iterator.next();
            if(keys.contains(word) && iterator.hasNext())
            {
                parsedTokens.add(new Pair<String,String>(word,(String) iterator.next()));
            }
        }
        return parsedTokens;
    }
}
