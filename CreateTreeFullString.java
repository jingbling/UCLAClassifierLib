import org.apache.commons.io.FilenameUtils;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class CreateTreeFullString {


    public CreateTreeFullString() {
        // Constructor - tbr
    }
    public static void usage() {
        System.out.println("usage: java CreateTree inputFile outputFile parameterDeclarations"+String.format("%n"));
        System.out.println(String.format("CreateTree reads in a Weka output file and creates a class for a classification tree."+String.format("%n")));
        System.out.println(String.format("Ex. :"+String.format("%n")));
        System.out.println(String.format("CreateTree WekaTreeOutput.txt classifyActivity.java double var, double avg, double[] fftCoeff"));
        System.exit(-1);
    }



    public static void main(String[] args) throws Exception {

//        String fileInput = "WekaOutLargeNum.txt";
//        String fileInput = "WekaOutputStatesRandomTree.txt";
//        String fileOutput = "TreeClassifier.java";
        String fileInput="null";
        String fileOutput="null";
        String classifierName="null";
//        List<String> classifierArgs = new ArrayList<String>();
        String[] classifierArgs = new String[args.length-3];
        Instances wekaData=null;
        String wekaOutput="null";
        StringBuffer parsedTree;
        Classifier wekaClassifier;
//        StringBuffer createdTree;
        Integer icount=3;


        try {
            // arg[0] = input file name
            fileInput = args[0].toString();
            fileOutput = args[1].toString();
            classifierName = args[2].toString();
            // save remaining arguments to pass into classifier call

//            System.out.println("Input args: "+args[0].toString()+" "+args[1].toString()+" "+args[2].toString());

            for (icount = 3; icount<=args.length-1; icount++) {
//                System.out.println("running for loop: "+icount.toString() + " of "+(args.length-3));
                classifierArgs[icount-3] = args[icount];
//                System.out.println("classifierArgs: " + classifierArgs[icount - 3].toString());
            }
        } catch (Exception e) {
            System.err.println("Error with input arguments");
            usage();
            System.exit(1);
        }

        wekaData = CreateWekaDataStructure(fileInput);

        wekaClassifier = RunWekaModel(wekaData, classifierName, classifierArgs);

        parsedTree = ParseTree(classifierName, wekaClassifier.toString(), 2);

//        System.out.println("parsedTree = " + parsedTree.toString());
//
//        // put some random args for now
        //String[] tempArgs = {"int","numLeaves","String","blah","double","somenumber"};
        String[] attributeArgs = ParseAttributeArgs(wekaData);
//
        // print attributes <debug>
//        System.out.println("Num attributes: " + wekaData.numAttributes());
//        for (icount = 0;icount < wekaData.numAttributes(); icount++) {
//            System.out.println("Attribute " + icount + ": " + wekaData.attribute(icount));
//        }
//        System.out.println("
//        System.out.println(", actual: " + wekaData.classAttribute().value((int) wekaData.instance(0).classValue()));

        writeJavaFile(parsedTree, attributeArgs, fileOutput);

//
//        // Call subroutine to parse input file
//        createdTree = ParseTree(fileInput, 2);
//        System.out.println(createdTree);

        // Call subroutine to write output file
//        writeJavaFile(createdTree, args, fileOutput);

    }

    private static String[] ParseAttributeArgs(Instances wekaInstance) {
        String[] returnAttrArgs = new String[(wekaInstance.numAttributes()-1)*2];
        Integer jcount = 0, argcount=0;
        String[] currentSplitLine = new String[3];
//        Scanner s = null;
//        String currentLine = "";
        // Parse assuming last attribute is the class attribute
        for (jcount=0; jcount<wekaInstance.numAttributes()-1; jcount++) {
            argcount = jcount*2;
            //parse each attribute type
//            s = new Scanner(new BufferedReader(new StringReader(wekaInstance.attribute(jcount).toString())));
//            currentLine = s.nextLine();
            currentSplitLine = wekaInstance.attribute(jcount).toString().split(" ");
            // check third split for attribute type
            if (currentSplitLine[2].equals("relational")) {
                System.err.println("Error, currently relational attributes are not handled, will return as string");
                returnAttrArgs[argcount] = "string";
            } else if (currentSplitLine[2].equals("numeric")) {
                returnAttrArgs[argcount] = "double";
            } else if (currentSplitLine[2].equals("date")) {
                returnAttrArgs[argcount] = "Date";
            } else {
                // treat all others as string
                returnAttrArgs[argcount] = "string";
            }
            // next save attribute name
            returnAttrArgs[argcount+1]= currentSplitLine[1];

//            System.out.println("Attribute " + jcount + ": " + currentSplitLine[1]);
//            System.out.println("Try parsing " + currentSplitLine[2]);
//            System.out.println("Saved args: " + returnAttrArgs[argcount] + " " + returnAttrArgs[argcount+1]);
//            System.out.println("size return: " + (wekaInstance.numAttributes()-1)*2);
        }

        return returnAttrArgs;
    }

    public static void writeJavaFile (StringBuffer treeCode, String[] parameters, String outputFile) {
        Integer icount;
        // Once finalOutput created, generate separate java class for running classifier
        try{
            FileWriter fstream = new FileWriter(outputFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("public class "+ FilenameUtils.removeExtension(outputFile)+ String.format("{ %n"));
            out.write("    public "+ FilenameUtils.removeExtension(outputFile)+ String.format("{ %n"));
            out.write("        //constructor - empty for now"+String.format("%n    }%n"));
            out.write("    public static String RunTree(");
            // Write parameters as variable declarations, expected in [type name] pairs
            // e.g. double length
            for (icount=0;icount<parameters.length; icount=icount+2) {
                // if this is the last pair, then don't add comma at the end
                if (icount == parameters.length-2){
                    out.write(parameters[icount]+" "+parameters[icount+1]);
                } else
                    out.write(parameters[icount]+" "+parameters[icount+1]+", ");
            }
            out.write(") {" + String.format("%n"));

            out.write(treeCode.toString());
            out.write("        return classifier;"+String.format("%n"));
            out.write("    }"+String.format("%n"));
            out.write("}"+String.format("%n"));
            //Close the output stream
            out.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }


    public static Instances CreateWekaDataStructure(String inputFile) {
        // This function is used to run the weka algorithms to generate a classifier output
        // Open input file and feed to Weka, accepted file types are arff,csv,xrff:
//        System.out.println("File type detected: " + FilenameUtils.getExtension(inputFile));
//        System.out.println("File to load: " + inputFile);
        Instances wekaData = null;
        try {
            DataSource wekaDataSource = new DataSource(inputFile);
            wekaData = wekaDataSource.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (wekaData.classIndex() == -1)
                wekaData.setClassIndex(wekaData.numAttributes() - 1);

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return wekaData;

    }

    public static Classifier RunWekaModel(Instances wekaInputData, String classifierName, String[] classifierArgs) throws Exception {
        // Depending on which classifier selected, run weka algorithms
        // classifierArgs contain additional options, such as incremental or batch
        Classifier cls = null;
        String output = "null";

        if (classifierName.toLowerCase().equals("j48")){
            cls = new weka.classifiers.trees.J48();
        } else if (classifierName.toLowerCase().equals("randomtree")) {
            cls = new weka.classifiers.trees.J48();
        } else if (classifierName.toLowerCase().equals("naivebayes")) {
            cls = new NaiveBayes();
        } else {
            // for now use default J48 tree
            cls = new weka.classifiers.trees.J48();
        }

        cls.buildClassifier(wekaInputData);
//        System.out.println(cls);
        //System.out.println(classifierArgs);

        output = cls.toString();

        System.out.println("output of cls: " + output);

        return cls;

    }
//    public void BuildWekaTree (String[] options, Instances wekaData) {
////        String[] options = new String[1];
////        options[0] = "-U";            // unpruned tree
//        J48 tree = new J48();         // new instance of tree
//        try {
//            tree.setOptions(options);     // set the options
//            tree.buildClassifier(wekaData);   // build classifier
//        } catch (Exception e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//    }


    public static StringBuffer ParseTree(String classifier, String input, int numTabs) {
        // this function expects a string with the classfier output and generates code
        // based on the type of classfier found.
        String[] inputSplit = input.split("\n");
        String line;
        StringBuffer modLine = new StringBuffer();
        StringBuffer finalOutput = new StringBuffer();
        Integer prevIfs=0, numIfs=0, isString=0;
        Integer i, counter=0,icount=0,lineStart=0;
        Integer endIndex=-1, stateIndexStart = -1;
        Scanner s = null;
        // define algorithms that have been implemented
        ArrayList TreeArrayList = new ArrayList();
        TreeArrayList.add("j48");
//    TreeArrayList.add("RandomTree")

        ArrayList BayesArrayList = new ArrayList();

//        System.out.println("classifier chosen: "+ classifier + " found = "+ TreeArrayList.contains(classifier));
//        System.out.println("inputSplit.length: "+ inputSplit.length );

        if (TreeArrayList.contains(classifier)) {
            // tree classifier found
            for (counter=2;counter<inputSplit.length;counter++) {
                //parse through classifier lines

//                System.out.println("inputSplit currently being checked: "+ inputSplit[counter] );

                if (inputSplit[counter].trim().isEmpty()) {
                    // line is blank, do nothing
                }
                else {
                    lineStart=0; //reset lineStart after end of each line read
                    prevIfs = numIfs; // save previous number of tabs to determine how many open ifs to close
                    numIfs=0; // reset numIfs after previous line read
                    for (i=0; inputSplit[counter].substring(i,i+1).equals("|"); i=i+4) {
    //                        System.out.println("found | at"+i);
                        numIfs=i/4+1;
                        lineStart=numIfs*4;
                    }
    //                    System.out.println("numIfs: "+numIfs+"prevIfs: "+prevIfs+"lineStart:"+lineStart);

                    // check for end of pruned tree, marked by "Number of Leaves  :"
                    // Must be placed here after prevIfs and numIfs refreshed so cleanup will be correct
                    if (inputSplit[counter].contains("Number of Leaves") || inputSplit[counter].contains("Size of the tree")) break;

                    // Need to check to see if logic in the current line is checking a state value
                    stateIndexStart = inputSplit[counter].indexOf(" = ");
                    isString = 0;
//                    System.out.println("line to parse:"+line + "stateIndexStart: "+stateIndexStart);
                    if (stateIndexStart > 0) {
                        // found = sign by itself, so insert another for boolean compare
                        inputSplit[counter] = new StringBuffer(inputSplit[counter]).insert(stateIndexStart + 1, "=").toString();
                        // Use first character after equals sign to determine if there is a number
                        if (inputSplit[counter].substring(stateIndexStart+3,stateIndexStart+5).contains("[0-9.]")){
                            //if number, leave as is, otherwise add quotes
                        } else {
                            isString = 1;
                            inputSplit[counter] = new StringBuffer(inputSplit[counter]).insert(stateIndexStart + 4, "\"").toString();
                        }

                    }
                    if (numIfs < prevIfs) {
                        // need to close out open ifs
                        for (icount=prevIfs;icount>numIfs;icount--) {
                            for (i=icount-1;i>0;i--){
                                modLine.append("    ");
                            }
                            modLine.append(String.format("}%n"));
                        }
                    }
                    if (numIfs > 0) {
                        for (i=numIfs;i>0;i--) {
    //                            System.out.println("prepending line");
                            modLine.append("    ");
                        }
                    }
    //                    System.out.println("modLine: "+modLine+"numIfs="+numIfs);
                    // After check for tabs, add if
                    modLine.append("if (");
                    // search for either where : appears, if it does not, create open "if" statement
                    endIndex = inputSplit[counter].indexOf(":");
                    if (endIndex > 0) {
                        if (isString == 1) {
                            modLine.append(inputSplit[counter].substring(lineStart,endIndex).trim()+String.format("\") { %n"));
                        } else {
                            modLine.append(inputSplit[counter].substring(lineStart,endIndex).trim()+String.format(") { %n"));
                        }
                        i = endIndex+1;
                        endIndex = inputSplit[counter].indexOf('(');

                        // add tabs based on how many if statements in we are
                        for (icount = numIfs+1; icount > 0; icount--) {
                            modLine.append("    ");
                        }
//                        System.out.println("line to write:"+inputSplit[counter].substring(i,endIndex));

                        modLine.append("classifier = \""+inputSplit[counter].substring(i,endIndex).trim()+String.format("\";%n"));
                        // add more tabs for end
                        for (icount = numIfs; icount > 0; icount--) {
                            modLine.append("    ");
                        }
                        modLine.append(String.format("} %n"));
                    }
                    else {
                        if (isString == 1) {
                            modLine.append(inputSplit[counter].substring(lineStart).trim()+String.format("\") {%n"));
                        } else {
//                            System.out.println("trimmed: "+inputSplit[counter].substring(lineStart).trim());
                            modLine.append(inputSplit[counter].substring(lineStart).trim()+String.format(") {%n"));
                        }
                    }

                }

    //                System.out.println(line);
    //                System.out.println(test);
            }
            // Perform final cleanup of any still open brackets
            if (numIfs < prevIfs) {
                // need to close out open ifs
                for (icount=prevIfs;icount>numIfs;icount--) {
                    for (i=icount-1;i>0;i--){
                        modLine.append("    ");
                    }
                    modLine.append(String.format("}%n"));
                }
            }

        }
        // At end, must prepend each line of generated tree with desired additional tabs
        // Check numTabs to determine number of tabs to prepend

        try {
            s = new Scanner(new BufferedReader(new StringReader(modLine.toString())));
            s.useDelimiter(",\\n");
            while (s.hasNext()) {
                line = s.nextLine();
                for (i=numTabs; i>0; i--) {
                    finalOutput.append("    ");
                }
                finalOutput.append(line+String.format("%n"));
            }
        } finally {
            if (s != null) {
                s.close();
            }
        }
//        System.out.println(finalOutput);
        return finalOutput;
    }

}