package com.bope;
import com.vdurmont.emoji.EmojiParser;
import com.bope.db.UserMongo;
import com.bope.db.UsersListMongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
public class DecryptoBot extends TelegramLongPollingBot {

        @Autowired
        private UsersListMongo usersListMongo;
        protected final Map<Long, Game> games = new HashMap<>();
        private static final Logger LOG = LoggerFactory.getLogger(DecryptoBot.class);

        @Value("${BOT_TOKEN}") private String BOT_TOKEN;
        @Value("${BOT_USER_NAME}") private String BOT_USER_NAME;

        @Value("${RED_TEAM_EMOJI}") private String RED_TEAM_EMOJI;
        @Value("${BLUE_TEAM_EMOJI}") private String BLUE_TEAM_EMOJI;
        @Value("${CROSS_EMOJI}") private String CROSS_EMOJI;
        @Value("${CHECK_MARK_EMOJI}") private String CHECK_MARK_EMOJI;
        @Value("${CALLBACK_SHOW_WORDS}") private String CALLBACK_SHOW_WORDS;
        @Value("${CALLBACK_SHOW_PROMPT}") private String CALLBACK_SHOW_PROMPT;
        @Value("${CALLBACK_SHOW_CODE}") private String CALLBACK_SHOW_CODE;
        @Value("${CALLBACK_SHOW_BLUE_PROMPTS}") private String CALLBACK_SHOW_BLUE_PROMPTS;
        @Value("${CALLBACK_SHOW_RED_PROMPTS}") private String CALLBACK_SHOW_RED_PROMPTS;
        @Value("${CORRECT_PROMPT}") private String CORRECT_PROMPT;
        @Value("${INCORRECT_PROMPT}") private String INCORRECT_PROMPT;
        @Value("${PROMPT_WAITING}") private String PROMPT_WAITING;
        @Value("${NOT_A_CAPTAIN}") private String NOT_A_CAPTAIN;
        @Value("${UNREGISTERED}") private String UNREGISTERED;
        @Value("${LESS_THEN_TWO_PLAYERS}") private String LESS_THEN_TWO_PLAYERS;
        @Value("${PLAYER_DUPLICATE}") private String PLAYER_DUPLICATE;
        @Value("${START_INSTRUCTION}") private String START_INSTRUCTION;
        @Value("${SHOW_WORDS_BUTTON}") private String SHOW_WORDS_BUTTON;
        @Value("${SHOW_PROMPT_BUTTON}") private String SHOW_PROMPT_BUTTON;
        @Value("${SHOW_CODE_BUTTON}") private String SHOW_CODE_BUTTON;
        @Value("${SHOW_BLUE_BUTTON}") private String SHOW_BLUE_BUTTON;
        @Value("${SHOW_RED_BUTTON}") private String SHOW_RED_BUTTON;
        @Value("${GAME_OVER}") private String GAME_OVER;
        @Value("${WAITING_RED}") private String WAITING_RED;
        @Value("${WAITING_BLUE}") private String WAITING_BLUE;
        @Value("${CORRECT_CODE}") private String CORRECT_CODE;
        @Value("${WRONG_CODE}") private String WRONG_CODE;



        public synchronized void answerCallbackQuery(String callbackId, String message) {
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(callbackId);
                answer.setText(message);
                answer.setShowAlert(true);
                try {
                        execute(answer);
                } catch (TelegramApiException e) {
                        e.printStackTrace();
                }
        }

        @Override
        public void onUpdateReceived(Update update) {

                long chatId;
                CallbackQuery callbackQuery = update.getCallbackQuery();
                if (callbackQuery != null) {
                        chatId = callbackQuery.getMessage().getChatId();
                        Game game = games.get(chatId);
                        if (game != null) {
                                String userName = callbackQuery.getFrom().getUserName();
                                if (game.isUserPlayer(userName)) {
                                        switch (callbackQuery.getData()) {
                                                case "CALLBACK_SHOW_WORDS" :
                                                        answerCallbackQuery(callbackQuery.getId(), game.getWordsListText(game.isPlayerBlue(userName)));
                                                        break;
                                                case "CALLBACK_SHOW_CODE" :
                                                        if (game.getCurrentCap().equals(userName))
                                                                answerCallbackQuery(callbackQuery.getId(), String.valueOf(game.getCurrentCode()));
                                                        else
                                                                answerCallbackQuery(callbackQuery.getId(), NOT_A_CAPTAIN);
                                                        break;
                                                case "CALLBACK_SHOW_PROMPT" :
                                                        if (game.isPromptSend())
                                                                answerCallbackQuery(callbackQuery.getId(), game.getCurrentPrompt());
                                                        else
                                                                answerCallbackQuery(callbackQuery.getId(), PROMPT_WAITING);
                                                        break;
                                                case "CALLBACK_SHOW_BLUE_PROMPTS":
                                                        answerCallbackQuery(callbackQuery.getId(), game.getPreviousPrompts(true));
                                                        break;
                                                case "CALLBACK_SHOW_RED_PROMPTS":
                                                        answerCallbackQuery(callbackQuery.getId(), game.getPreviousPrompts(false));
                                                        break;
                                        }
                                }
                        }
                        return;
                }

                Message message = update.getMessage();
                if (message == null)
                        return;
                chatId = message.getChatId();
                String text = message.getText();
                String userName = message.getFrom().getUserName();

                if (text.startsWith("/"))
                        text = text.substring(1);

                if (text.equals("start")) {
                        botStartCommand(chatId, userName, message.getFrom().getId());
                        return;
                }

                if (text.startsWith("start")) {
                        text = text.substring(7);

                        Set<String> blueTeam = new HashSet<>();
                        Set<String> redTeam = new HashSet<>();
                        Set<String> unregisteredUsers = new HashSet<>();

                        boolean isFirstLine = true;

                        for (String line : text.split("\n")) {
                                for (String player : line.split("@")) {
                                        player = player.trim();
                                        if (player.equals(""))
                                                continue;
                                        UserMongo userMongo = usersListMongo.findByUserName(player);
                                        if (userMongo != null) {
                                                if (isFirstLine) {
                                                        blueTeam.add(userMongo.getUserName());
                                                } else {
                                                        redTeam.add(userMongo.getUserName());
                                                }
                                        } else {
                                                unregisteredUsers.add(player);
                                        }
                                }
                                isFirstLine = false;
                        }

                        if (!unregisteredUsers.isEmpty()) {
                                StringBuilder sb = new StringBuilder(UNREGISTERED);
                                for (String unregistered : unregisteredUsers)
                                        sb.append("@").append(unregistered).append(" ");
                                sendSimpleMessage(sb.toString(), chatId);
                                return;
                        }

                        if (blueTeam.size() < 2 || redTeam.size() < 2) {
                                sendSimpleMessage(LESS_THEN_TWO_PLAYERS, chatId);
                                return;
                        }

                        for (String user : blueTeam) {
                                if (redTeam.contains(user)) {
                                        sendSimpleMessage(PLAYER_DUPLICATE, chatId);
                                        return;
                                }
                        }
                        games.put(chatId, new Game(chatId, blueTeam, redTeam));
                        sendBoard(chatId);
                        return;
                }


                Game game = games.get(chatId);
                if (game == null)
                        return;


                if (text.equals("status")) {
                        sendBoard(chatId);
                        return;
                }


                if (game.getCurrentCap().equals(userName) && !game.isPromptSend()) {
                        if (Game.isPromptCorrect(text)) {
                                game.setCurrentPrompt(text);
                                sendBoard(CORRECT_PROMPT, chatId);
                        } else
                                sendSimpleMessage(INCORRECT_PROMPT, chatId);
                }



                if (isTextCodeCorrect(text) && !game.getCurrentCap().equals(userName) && game.isPromptSend()) {
                        int code = Integer.parseInt(text);


                        if (game.isBlueTurn()) {
                                if (game.isPlayerRed(userName) && !game.isOppositeCodeSet()) {
                                        if (game.getCurrentCode() == code) {
                                                if (game.isRedWin())
                                                        gameOver(chatId, false);
                                                else {
                                                        game.setRedWin(true);
                                                        sendBoard(String.format(CORRECT_CODE, game.getNextCap()), chatId);
                                                }
                                        } else {
                                                game.setOppositeCodeSet(true);
                                                sendBoard(WRONG_CODE, chatId);
                                        }
                                } else if (game.isPlayerBlue(userName) && game.isOppositeCodeSet()) {
                                        if (game.getCurrentCode() == code) {
                                                sendBoard(String.format(CORRECT_CODE, game.getNextCap()), chatId);
                                        } else {
                                                if (game.isBlueFail())
                                                        gameOver(chatId, false);
                                                else {
                                                        game.setBlueFail(true);
                                                        sendBoard(String.format(WRONG_CODE, game.getNextCap()), chatId);
                                                }
                                        }
                                }
                        } else {
                                if (game.isPlayerBlue(userName) && !game.isOppositeCodeSet()) {
                                        if (game.getCurrentCode() == code) {
                                                if (game.isBlueWin())
                                                        gameOver(chatId, true);
                                                else {
                                                        game.setBlueWin(true);
                                                        sendBoard(String.format(CORRECT_CODE, game.getNextCap()), chatId);
                                                }
                                        } else {
                                                game.setOppositeCodeSet(true);
                                                sendBoard(WRONG_CODE, chatId);
                                        }
                                } else if (game.isPlayerRed(userName) && game.isOppositeCodeSet()) {
                                        if (game.getCurrentCode() == code) {
                                                sendBoard(String.format(CORRECT_CODE, game.getNextCap()), chatId);
                                        } else {
                                                if (game.isRedFail())
                                                        gameOver(chatId, true);
                                                else {
                                                        game.setRedFail(true);
                                                        sendBoard(String.format(WRONG_CODE, game.getNextCap()), chatId);
                                                }
                                        }
                                }
                        }
                }
        }


        private void sendBoard(long chatId) {
                sendBoard("", chatId);
        }


        private void sendBoard(String message, long chatId) {
                Game game = games.get(chatId);
                StringBuilder sb = new StringBuilder();

                if (message.equals("")) {
                        sb.append(BLUE_TEAM_EMOJI).append(" Blue team ");
                        if (game.isBlueWin())
                                sb.append(CHECK_MARK_EMOJI);
                        if (game.isBlueFail())
                                sb.append(CROSS_EMOJI);
                        sb.append(": ");
                        for (String player : game.getBlueTeam())
                                sb.append(" @").append(player);

                        sb.append("\n").append(RED_TEAM_EMOJI).append(" Red team ");
                        if (game.isRedWin())
                                sb.append(CHECK_MARK_EMOJI);
                        if (game.isRedFail())
                                sb.append(CROSS_EMOJI);
                        sb.append(": ");
                        for (String player : game.getRedTeam())
                                sb.append(" @").append(player);
                } else
                        sb.append(message);

                sb.append("\nCaptain: ");
                sb.append(game.isBlueTurn() ? BLUE_TEAM_EMOJI : RED_TEAM_EMOJI);
                sb.append(" @").append(game.getCurrentCap()).append("\n");

                if (!game.isPromptSend()) {
                        sb.append(PROMPT_WAITING);
                } else {
                        if ((game.isBlueTurn() && game.isOppositeCodeSet()) || (!game.isBlueTurn() && !game.isOppositeCodeSet()))
                                sb.append(WAITING_BLUE);
                        else
                                sb.append(WAITING_RED);
                }

                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                List<InlineKeyboardButton> buttons1 = new ArrayList<>();
                buttons1.add(new InlineKeyboardButton().setText(SHOW_WORDS_BUTTON).setCallbackData(CALLBACK_SHOW_WORDS));
                buttons1.add(new InlineKeyboardButton().setText(SHOW_PROMPT_BUTTON).setCallbackData(CALLBACK_SHOW_PROMPT));
                buttons1.add(new InlineKeyboardButton().setText(SHOW_CODE_BUTTON).setCallbackData(CALLBACK_SHOW_CODE));

                List<InlineKeyboardButton> buttons2 = new ArrayList<>();
                buttons2.add(new InlineKeyboardButton().setText(SHOW_BLUE_BUTTON).setCallbackData(CALLBACK_SHOW_BLUE_PROMPTS));
                buttons2.add(new InlineKeyboardButton().setText(SHOW_RED_BUTTON).setCallbackData(CALLBACK_SHOW_RED_PROMPTS));

                buttons.add(buttons1);
                buttons.add(buttons2);

                InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
                markupKeyboard.setKeyboard(buttons);

                try {
                        execute(new SendMessage().setChatId(chatId).setText(EmojiParser.parseToUnicode(sb.toString())).setReplyMarkup(markupKeyboard));
                } catch (TelegramApiException e) {
                        e.printStackTrace();
                }




        }

        private void gameOver(long chatId, boolean isBlueTeamWin) {
                games.remove(chatId);
                sendSimpleMessage(String.format(GAME_OVER, (isBlueTeamWin ? "Blue" : "Red")), chatId);
        }

        private boolean isTextCodeCorrect(String text) {
                Set<Character> set = new HashSet<>();
                for (char c : text.toCharArray())
                        set.add(c);
                if (set.size() != 3)
                        return false;
                set.removeAll(Arrays.asList('1', '2', '3', '4'));
                return set.size() == 0;
        }

        private void botStartCommand(long chatId, String userName, long userId) {
                if (chatId == userId && usersListMongo.findByUserName(userName) == null) {
                        usersListMongo.save(new UserMongo(userName, String.valueOf(userId)));
                }
                sendSimpleMessage(START_INSTRUCTION, chatId);
        }

        private void sendSimpleMessage(String text, long chatId) {
                SendMessage message = new SendMessage().setChatId(chatId).setText(text);
                try {
                        execute(message);
                } catch (TelegramApiException e) {
                        e.printStackTrace();
                }
        }

        @Override
        public String getBotUsername() {
                return BOT_USER_NAME;
        }

        @Override
        public String getBotToken() {
                return BOT_TOKEN;
        }
}