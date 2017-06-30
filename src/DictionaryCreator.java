import com.sun.org.apache.xpath.internal.SourceTree;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class DictionaryCreator {

    private ArrayList<String> stanfordPaths=new ArrayList<String>();
    HashMap<String,String> wordToLemma=new HashMap<String,String>();
    HashMap<String,Float[]> vocab=new HashMap<String,Float[]>();
    HashMap<String,Float> dict=new HashMap<String,Float>();

    public static void main(String[] args) {
        DictionaryCreator dc=new DictionaryCreator();
        dc.readList();
        dc.buildVocab();
        dc.buildDictionary();
        dc.writeToFile();
    }

    private void writeToFile() {
        System.out.println("Writing file");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("../output/Dictionary.txt", "UTF-8");
            Iterator<String> itr=wordToLemma.keySet().iterator();
            while(itr.hasNext()){
                String word=itr.next();
                String lemma=wordToLemma.get(word);
                Float frq=dict.get(word);
                writer.println(word+" "+lemma+" "+frq);
                if(!word.equals(word.toLowerCase())) {
                    writer.println(word.toLowerCase() + " " + lemma + " " + frq);
                }
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void buildDictionary(){
        System.out.println("Building dictionary");
        float min=Float.POSITIVE_INFINITY;
        float max=Float.NEGATIVE_INFINITY;
        Iterator<String> itr=vocab.keySet().iterator();
        while(itr.hasNext()){
            String word=itr.next();
            Float[] values=vocab.get(word);
            //System.out.println(word+" "+values[0]+" "+values[1]);
            float frq=values[0]/values[1];
            min=Math.min(frq,min);
            max=Math.max(frq,max);
            dict.put(word,frq);
        }

        //Adjust curve
        itr=dict.keySet().iterator();
        while(itr.hasNext()) {
            String word = itr.next();
            float x=dict.get(word);
            float y=(((x-min)*(1-min))/(max-min))+min;
            dict.put(word,y);
        }

    }

    private void buildVocab(){
        System.out.println("Building vocabulary");
        for (int i = 0; i < stanfordPaths.size(); i++) {
            ArrayList<Element> sentences=loadStanfordXML(stanfordPaths.get(i));
            HashMap<String,Float> abstractVocab=new HashMap<String,Float>(); //Vocablary of this abstract
            int abstractLength=0;

            for (int j = 0; j <sentences.size() ; j++) {
                Element sentence=sentences.get(j);

                NodeList wList=sentence.getElementsByTagName("word"); //word list
                NodeList lList=sentence.getElementsByTagName("lemma");  //lemma list

                for (int k = 0;k <wList.getLength() ; k++) { //Iterating among words
                    try {
                        Node wNode = wList.item(k);
                        if (wNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                            Node lNode = lList.item(k);
                            String word = wNode.getTextContent();
                            word=word.toLowerCase();
                            char c=word.charAt(0);
                            if(((c>='a')&&(c<='z'))||((c>='0')&&(c<='9'))) {
                                if (lList != null) {
                                    wordToLemma.put(word, lNode.getTextContent());
                                } else {
                                    wordToLemma.put(word, word);
                                }
                                Float val = abstractVocab.get(word);
                                if (val == null) {
                                    val = 0.0f;
                                }
                                val += 1.0f;
                                abstractVocab.put(word, val);
                                abstractLength++;
                            }
                        }
                    }
                    catch(Exception e){

                    }
                }

            }

            //Update the full vocab
            Iterator<String>  itr=abstractVocab.keySet().iterator();
            while(itr.hasNext()){
                String word=itr.next();
                Float abVal=abstractVocab.get(word);
               // System.out.println(word+" "+abVal);

                Float[] value=vocab.get(word);
                if(value==null){
                    value=new  Float[]{0.0f,0.0f};
                }
                value[0]+=(abVal/abstractLength);
                value[1]+=1.0f;

                vocab.put(word,value);
            }
           // System.out.println();
        }
    }



    private void readList(){
        File folder = new File("../output/02_Stanford");
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            stanfordPaths.add(listOfFiles[i].getPath());
           //System.out.println(listOfFiles[i].getName());
        }
    }


    public ArrayList<Element> loadStanfordXML(String stanfordPath) {
        ArrayList<Element> nodes=new ArrayList<Element>();
        File fXmlFile = new File(stanfordPath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder =dbFactory.newDocumentBuilder();
            Document doc =dBuilder.parse(fXmlFile);
            NodeList nList= doc.getElementsByTagName("sentence");
            Element eElement=null;
            String idString="";
            for (int i = 0; i <nList.getLength() ; i++) {
                org.w3c.dom.Node nNode = nList.item(i);
                if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    eElement = (Element) nNode;
                    idString=eElement.getAttribute("id");
                    if(idString.length()>0) {
                        int id = Integer.parseInt(eElement.getAttribute("id"));
                        nodes.add(eElement);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nodes;
    }
}
