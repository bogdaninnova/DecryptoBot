package com.bope;

import com.bope.db.UserMongo;
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


    private final HashMap<Integer, String> blueCodes = new HashMap<>();
    private final HashMap<Integer, String> redCodes = new HashMap<>();
    private final List<UserMongo> blueTeam = new ArrayList<>();
    private final List<UserMongo> redTeam = new ArrayList<>();

    private List<WordMongo> blueWords;
    private List<WordMongo> redWords;


    private final List<WordMongo> allWordList;
    private static final Random rand = new Random();

    private final List<Integer> blueCodesList = new ArrayList<>();
    private final List<Integer> redCodesList = new ArrayList<>();

    public Game(long chatId, Set<UserMongo> blueTeam, Set<UserMongo> redTeam) {
        this.chatId = chatId;
        allWordList = Main.ctx.getBean(WordsListMongo.class).findByLang("rus");
        this.blueTeam.addAll(blueTeam);
        this.redTeam.addAll(redTeam);
        startNewGame();
    }


    public void startNewGame() {
        this.isBlueTurn = rand.nextBoolean();
        this.blueWords = getWords();
        this.redWords = getWords();
    }



    public UserMongo getCurrentCap() {
        return isBlueTurn ? blueTeam.get(round%blueTeam.size()) : redTeam.get(round%redTeam.size());
    }

    public UserMongo getNextCap() {
        if (isBlueTurn)
            round++;
        isBlueTurn = !isBlueTurn;
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
        for (UserMongo user : redTeam)
            if (user.getUserName().equals(userName))
                return true;
        return isPlayerBlue(userName);
    }


    public boolean isPlayerBlue(String userName) {
        for (UserMongo user : blueTeam)
            if (user.getUserName().equals(userName))
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

}
