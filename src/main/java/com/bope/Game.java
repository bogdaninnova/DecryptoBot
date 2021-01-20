package com.bope;

import com.bope.db.WordMongo;
import com.bope.db.WordsListMongo;

import java.util.*;

public class Game {


    private final long chatId;


    private boolean blueFail = false;
    private boolean blueWin = false;
    private boolean redFail = false;
    private boolean redWin = false;


    private int currentCode;
    private int round = 0;
    private boolean isBlueTurn;
    private boolean isOppositeCodeSet = false;
    private boolean isPromptSend = false;

    private final HashMap<Integer, String> blueCodes;
    private final HashMap<Integer, String> redCodes;

    private final List<String> blueTeam = new ArrayList<>();
    private final List<String> redTeam = new ArrayList<>();

    private List<WordMongo> blueWords;
    private List<WordMongo> redWords;

    private String currentPrompt;

    private final List<WordMongo> allWordList;
    private static final Random rand = new Random();

    private final List<Integer> blueCodesList = new ArrayList<>();
    private final List<Integer> redCodesList = new ArrayList<>();

    public Game(long chatId, Set<String> blueTeam, Set<String> redTeam) {
        this.chatId = chatId;
        allWordList = Main.ctx.getBean(WordsListMongo.class).findByLang("rus");
        this.blueTeam.addAll(blueTeam);
        this.redTeam.addAll(redTeam);
        startNewGame();

        blueCodes = new HashMap<>();
        blueCodes.put(1, "");
        blueCodes.put(2, "");
        blueCodes.put(3, "");
        blueCodes.put(4, "");

        redCodes = new HashMap<>();
        redCodes.put(1, "");
        redCodes.put(2, "");
        redCodes.put(3, "");
        redCodes.put(4, "");

    }


    public void startNewGame() {
        this.isBlueTurn = rand.nextBoolean();
        this.blueWords = getWords();
        this.redWords = getWords();
        this.currentCode = generateCode(isBlueTurn);
    }



    public String getCurrentCap() {
        return isBlueTurn ? blueTeam.get(round%blueTeam.size()) : redTeam.get(round%redTeam.size());
    }

    public String getNextCap() {
        if (isBlueTurn)
            round++;
        isBlueTurn = !isBlueTurn;
        currentCode = generateCode(isBlueTurn);
        isPromptSend = false;
        isOppositeCodeSet = false;
        addPrompts();
        return getCurrentCap();
    }

    private List<WordMongo> getWords() {
        List<WordMongo> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            WordMongo word = allWordList.remove(rand.nextInt(allWordList.size()-1));
            result.add(word);
        }
        return result;
    }

    public int generateCode(boolean isBlue) {
        ArrayList<Integer> list;
        while(true) {
            list = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
            int code =  100 * list.remove(rand.nextInt(list.size())) +
                        10 * list.remove(rand.nextInt(list.size())) +
                        list.remove(rand.nextInt(list.size()));

            if (isBlue && !blueCodesList.contains(code)) {
                blueCodesList.add(code);
                return code;
            } else if (!isBlue && !redCodesList.contains(code)) {
                redCodesList.add(code);
                return code;
            }
        }
    }


    public boolean isUserPlayer(String userName) {
        return isPlayerBlue(userName) || isPlayerRed(userName);
    }


    public boolean isPlayerBlue(String userName) {
        for (String user : blueTeam)
            if (user.equals(userName))
                return true;
            return false;
    }

    public boolean isPlayerRed(String userName) {
        for (String user : redTeam)
            if (user.equals(userName))
                return true;
        return false;
    }


    public long getChatId() {
        return chatId;
    }

    public String getWordsListText(boolean isBlue) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (WordMongo word : (isBlue ? blueWords : redWords)) {
            if (i > 1)
                sb.append("\n");
            sb.append(i++).append(". ").append(word.getWord());
        }
        return sb.toString();
    }

    public int getCurrentCode() {
        return currentCode;
    }

    public boolean isBlueTurn() {
        return isBlueTurn;
    }

    public boolean isRedTurn() {
        return !isBlueTurn;
    }

    public boolean isOppositeCodeSet() {
        return isOppositeCodeSet;
    }

    public void setOppositeCodeSet(boolean oppositeCodeSet) {
        isOppositeCodeSet = oppositeCodeSet;
    }

    public boolean isBlueFail() {
        return blueFail;
    }

    public void setBlueFail(boolean blueFail) {
        this.blueFail = blueFail;
    }

    public boolean isBlueWin() {
        return blueWin;
    }

    public void setBlueWin(boolean blueWin) {
        this.blueWin = blueWin;
    }

    public boolean isRedFail() {
        return redFail;
    }

    public void setRedFail(boolean redFail) {
        this.redFail = redFail;
    }

    public boolean isRedWin() {
        return redWin;
    }

    public void setRedWin(boolean redWin) {
        this.redWin = redWin;
    }

    public boolean isPromptSend() {
        return isPromptSend;
    }

    public List<String> getBlueTeam() {
        return blueTeam;
    }

    public List<String> getRedTeam() {
        return redTeam;
    }

    public void setPromptSend(boolean promptSend) {
        isPromptSend = promptSend;
    }

    public HashMap<Integer, String> getBlueCodes() {
        return blueCodes;
    }

    public HashMap<Integer, String> getRedCodes() {
        return redCodes;
    }

    public void addPrompts() {
        ArrayList<String> prompts = parsePrompts(currentPrompt);
        for (int i = 0; i <= 2; i++) {
            int code = Integer.parseInt(String.valueOf(currentCode).substring(i, i+1));
            if (isBlueTurn)
                blueCodes.put(code, prompts.get(i) + (blueCodes.get(code).equals("") ? "" : ", ") + blueCodes.get(code));
            else
                redCodes.put(code, prompts.get(i) + (redCodes.get(code).equals("") ? "" : ", ") + redCodes.get(code));
        }
    }

    public String getCurrentPrompt() {
        return currentPrompt;
    }

    public void setCurrentPrompt(String currentPrompt) {
        setPromptSend(true);
        this.currentPrompt = currentPrompt;
    }

    private static ArrayList<String> parsePrompts(String text) {
        String[] arr = text.split(" ");
        if (arr.length != 3)
            return null;
        return new ArrayList<>(Arrays.asList(arr));
    }

    public static boolean isPromptCorrect(String text) {
        return parsePrompts(text) != null;
    }
}
