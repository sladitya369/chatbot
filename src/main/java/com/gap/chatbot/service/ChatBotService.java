package com.gap.chatbot.service;

import opennlp.tools.doccat.*;
import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.*;
import opennlp.tools.util.model.ModelUtil;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class ChatBotService {

    //private static Map<String, String> questionAnswer = new HashMap<>();

    /*
     * Define answers for each given category.
     */
    /*static {
        questionAnswer.put("greeting", "Hello, how can I help you?");
        questionAnswer.put("hr-timesheet",
                "timesheet url.");
        questionAnswer.put("hr-policy", "policy url");
        questionAnswer.put("conversation-continue", "What else can I help you with?");
        questionAnswer.put("conversation-complete", "Nice chatting with you. Bbye.");

    }*/

    public String connectWithChatBot(String input) throws FileNotFoundException, IOException, InterruptedException {

        HashMap<String, String> questionAnswer = new HashMap<>();
        String line;
        File file = new File(System.getProperty("user.dir")  + "/tag.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while ((line = reader.readLine()) != null) {
                String[] keyValuePair = line.split(":", 2);
                if (keyValuePair.length > 1) {
                    String key = keyValuePair[0];
                    String value = keyValuePair[1];
                    questionAnswer.put(key, value);
                } else {
                    System.out.println("No Key:Value found in line, ignoring: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Train categorizer model to the training data we created.
        DoccatModel model = trainCategorizerModel();

        // Get chat input from user.
        System.out.println("##### You:");

        // Break users chat input into sentences using sentence detection.
        String[] sentences = breakSentences(input);

        String answer = "";
        boolean conversationComplete = false;

        // Loop through sentences.
        for (String sentence : sentences) {

            // Separate words from each sentence using tokenizer.
            String[] tokens = tokenizeSentence(sentence);

            // Tag separated words with POS tags to understand their gramatical structure.
            String[] posTags = detectPOSTags(tokens);

            // Lemmatize each word so that its easy to categorize.
            String[] lemmas = lemmatizeTokens(tokens, posTags);

            // Determine BEST category using lemmatized tokens used a mode that we trained
            // at start.
            String category = detectCategory(model, lemmas);

            // Get predefined answer from given category & add to answer.
            answer = answer + " " + questionAnswer.get(category);

            // If category conversation-complete, we will end chat conversation.
            if ("conversation-complete".equals(category)) {
                conversationComplete = true;
            }
        }

        // Print answer back to user. If conversation is marked as complete, then end
        // loop & program.
        System.out.println("##### Chat Bot: " + answer);
        /*if (conversationComplete) {
            break;
        }*/
        return answer;

    }

    /**
     * Train categorizer model as per the category sample training data we created.
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private DoccatModel trainCategorizerModel() throws FileNotFoundException, IOException {
        // faq-categorizer.txt is a custom training data with categories as per our chat
        // requirements.
        //Resource resource = new ClassPathResource("faq-categorizer.txt");
        //FileReader fileReader = new FileReader(resource.getFile());
        //InputStream inputStream = getClass().getResourceAsStream("/faq-categorizer.txt");
        InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(new File(System.getProperty("user.dir")  + "/faq-categorizer.txt"));
        ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
        ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

        DoccatFactory factory = new DoccatFactory(new FeatureGenerator[] { new BagOfWordsFeatureGenerator() });

        TrainingParameters params = ModelUtil.createDefaultTrainingParameters();
        params.put(TrainingParameters.CUTOFF_PARAM, 0);

        // Train a model with classifications from above file.
        DoccatModel model = DocumentCategorizerME.train("en", sampleStream, params, factory);
        return model;
    }

    /**
     * Detect category using given token. Use categorizer feature of Apache OpenNLP.
     *
     * @param model
     * @param finalTokens
     * @return
     * @throws IOException
     */
    private String detectCategory(DoccatModel model, String[] finalTokens) throws IOException {

        // Initialize document categorizer tool
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);

        // Get best possible category.
        double[] probabilitiesOfOutcomes = myCategorizer.categorize(finalTokens);
        String category = myCategorizer.getBestCategory(probabilitiesOfOutcomes);
        System.out.println("Category: " + category);

        return category;

    }

    /**
     * Break data into sentences using sentence detection feature of Apache OpenNLP.
     *
     * @param data
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private String[] breakSentences(String data) throws FileNotFoundException, IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        //File file = ResourceUtils.getFile("src/main/resources/en-sent.bin");
        //InputStream modelIn = new FileInputStream(file);
        InputStream modelIn = new FileInputStream(new File(System.getProperty("user.dir")  + "/en-sent.bin"));
        SentenceDetectorME myCategorizer = new SentenceDetectorME(new SentenceModel(modelIn));

        String[] sentences = myCategorizer.sentDetect(data);
        System.out.println("Sentence Detection: " + Arrays.stream(sentences).collect(Collectors.joining(" | ")));

        return sentences;
    }

    /**
     * Break sentence into words & punctuation marks using tokenizer feature of
     * Apache OpenNLP.
     *
     * @param sentence
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private String[] tokenizeSentence(String sentence) throws FileNotFoundException, IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream(System.getProperty("user.dir")  + "/en-token.bin")) {

            // Initialize tokenizer tool
            TokenizerME myCategorizer = new TokenizerME(new TokenizerModel(modelIn));

            // Tokenize sentence.
            String[] tokens = myCategorizer.tokenize(sentence);
            System.out.println("Tokenizer : " + Arrays.stream(tokens).collect(Collectors.joining(" | ")));

            return tokens;

        }
    }

    /**
     * Find part-of-speech or POS tags of all tokens using POS tagger feature of
     * Apache OpenNLP.
     *
     * @param tokens
     * @return
     * @throws IOException
     */
    private String[] detectPOSTags(String[] tokens) throws IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream(System.getProperty("user.dir")  + "/en-pos-maxent.bin")) {

            // Initialize POS tagger tool
            POSTaggerME myCategorizer = new POSTaggerME(new POSModel(modelIn));

            // Tag sentence.
            String[] posTokens = myCategorizer.tag(tokens);
            System.out.println("POS Tags : " + Arrays.stream(posTokens).collect(Collectors.joining(" | ")));

            return posTokens;

        }

    }

    /**
     * Find lemma of tokens using lemmatizer feature of Apache OpenNLP.
     *
     * @param tokens
     * @param posTags
     * @return
     * @throws InvalidFormatException
     * @throws IOException
     */
    private String[] lemmatizeTokens(String[] tokens, String[] posTags)
            throws InvalidFormatException, IOException {
        // Better to read file once at start of program & store model in instance
        // variable. but keeping here for simplicity in understanding.
        try (InputStream modelIn = new FileInputStream(System.getProperty("user.dir")  + "/en-lemmatizer.bin")) {

            // Tag sentence.
            LemmatizerME myCategorizer = new LemmatizerME(new LemmatizerModel(modelIn));
            String[] lemmaTokens = myCategorizer.lemmatize(tokens, posTags);
            System.out.println("Lemmatizer : " + Arrays.stream(lemmaTokens).collect(Collectors.joining(" | ")));

            return lemmaTokens;

        }
    }
}
