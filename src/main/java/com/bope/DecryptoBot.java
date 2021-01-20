package com.bope;
import com.bope.db.UserMongo;
import com.bope.db.UsersListMongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.vdurmont.emoji.EmojiParser;

import java.util.*;

@Component
public class DecryptoBot extends TelegramLongPollingBot {

        @Autowired
        private UsersListMongo usersListMongo;
        protected final Map<Long, Game> games = new HashMap<>();
        private static final Logger LOG = LoggerFactory.getLogger(DecryptoBot.class);



        private static final String RED_TEAM_EMOJI = ":closed_book:";
        private static final String BLUE_TEAM_EMOJI = ":blue_book:";
        private static final String CROSS_EMOJI = ":x:";
        private static final String CHECK_MARK_EMOJI = ":white_check_mark:";



        public DecryptoBot() {


        }


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
                                                                answerCallbackQuery(callbackQuery.getId(), "You are not a captain now");
                                                        break;
                                                case "CALLBACK_SHOW_PROMPT" :
                                                        if (game.isPromptSend())
                                                                answerCallbackQuery(callbackQuery.getId(), game.getCurrentPrompt());
                                                        else
                                                                answerCallbackQuery(callbackQuery.getId(), "Waiting for prompt");
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
                                StringBuilder sb = new StringBuilder("Unregistered users:\n");
                                for (String unregistered : unregisteredUsers)
                                        sb.append("@").append(unregistered).append(" ");
                                sendSimpleMessage(sb.toString(), chatId);
                                return;
                        }

                        if (blueTeam.size() < 2 || redTeam.size() < 2) {
                                sendSimpleMessage("In every team should be more then one player!", chatId);
                                return;
                        }

                        for (String user : blueTeam) {
                                if (redTeam.contains(user)) {
                                        sendSimpleMessage("Every player should be presents in one team only!", chatId);
                                        return;
                                }
                        }
                        startGame(new Game(chatId, blueTeam, redTeam));
                        return;
                }


                Game game = games.get(chatId);
                if (game == null)
                        return;


                if (text.equals("status")) {
                        sendCurrentStatus(chatId);
                        return;
                }


                if (game.getCurrentCap().equals(userName) && !game.isPromptSend()) {
                        if (Game.isPromptCorrect(text)) {
                                game.setCurrentPrompt(text);
                                sendSimpleMessage("Prompts are correct", chatId);
                        } else
                                sendSimpleMessage("Incorrect prompts", chatId);
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
                                                        sendSimpleMessage("Your answer is correct!\nNow is Red turn - captain is @" + game.getNextCap(), chatId);
                                                }
                                        } else {
                                                game.setOppositeCodeSet(true);
                                                sendSimpleMessage("Code is wrong!\nNow is Blue team try to guess", chatId);
                                        }
                                } else if (game.isPlayerBlue(userName) && game.isOppositeCodeSet()) {
                                        if (game.getCurrentCode() == code) {
                                                sendSimpleMessage("Code is correct!\nNow is Red turn: @" + game.getNextCap() + "'s turn.", chatId);
                                        } else {
                                                if (game.isBlueFail())
                                                        gameOver(chatId, false);
                                                else {
                                                        game.setBlueFail(true);
                                                        sendSimpleMessage("Your answer is wrong!\nNow is Red turn: @" + game.getNextCap() + "'s turn.", chatId);
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
                                                        sendSimpleMessage("Your answer is correct!\nNow is Blue turn - captain is @" + game.getNextCap(), chatId);
                                                }
                                        } else {
                                                game.setOppositeCodeSet(true);
                                                sendSimpleMessage("Code is wrong!\nNow is Red team try to guess", chatId);
                                        }
                                } else if (game.isPlayerRed(userName) && game.isOppositeCodeSet()) {
                                        if (game.getCurrentCode() == code) {
                                                sendSimpleMessage("Code is correct!\nNow is Blue turn: @" + game.getNextCap() + "'s turn.", chatId);
                                        } else {
                                                if (game.isRedFail())
                                                        gameOver(chatId, true);
                                                else {
                                                        game.setRedFail(true);
                                                        sendSimpleMessage("Your answer is wrong!\nNow is Blue turn: @" + game.getNextCap() + "'s turn.", chatId);
                                                }
                                        }
                                }
                        }
                }
        }





        private void sendCurrentStatus(long chatId) {
                Game game = games.get(chatId);
                StringBuilder sb = new StringBuilder();

                sb.append(" ").append(BLUE_TEAM_EMOJI).append(" Blue team");
                if (game.isBlueWin())
                        sb.append(CHECK_MARK_EMOJI);
                if (game.isBlueFail())
                        sb.append(CROSS_EMOJI);
                sb.append(":\n");
                for (String player : game.getBlueTeam())
                        sb.append(" @").append(player);

                sb.append("\n\n").append(RED_TEAM_EMOJI).append(" Red team");
                if (game.isRedWin())
                        sb.append(CHECK_MARK_EMOJI);
                if (game.isRedFail())
                        sb.append(CROSS_EMOJI);
                sb.append(":\n");
                for (String player : game.getRedTeam())
                        sb.append(" @").append(player);

                sb.append("\n\nNow is ");
                sb.append(game.isBlueTurn() ? "Blue" : "Red");
                sb.append(" Team's turn!\n@");
                sb.append(game.getCurrentCap());
                sb.append(" is current captain.\n\n");

                sb.append("Blue prompts:\n");
                sb.append("1. ").append(game.getBlueCodes().get(1)).append("\n");
                sb.append("2. ").append(game.getBlueCodes().get(2)).append("\n");
                sb.append("3. ").append(game.getBlueCodes().get(3)).append("\n");
                sb.append("4. ").append(game.getBlueCodes().get(4)).append("\n\n");

                sb.append("Red prompts:\n");
                sb.append("1. ").append(game.getRedCodes().get(1)).append("\n");
                sb.append("2. ").append(game.getRedCodes().get(2)).append("\n");
                sb.append("3. ").append(game.getRedCodes().get(3)).append("\n");
                sb.append("4. ").append(game.getRedCodes().get(4)).append("\n\n");

                if (!game.isPromptSend()) {
                        sb.append("Waiting for prompt from captain!");
                } else {
                        if ((game.isBlueTurn() && game.isOppositeCodeSet()) || (!game.isBlueTurn() && !game.isOppositeCodeSet()))
                                sb.append("Waiting for code from Blue team!");
                        else
                                sb.append("Waiting for code from Red team!");
                }

                sendBoard(EmojiParser.parseToUnicode(sb.toString()), game.getChatId());
        }



        private void gameOver(long chatId, boolean isBlueTeamWin) {
                games.remove(chatId);
                sendSimpleMessage("Game Over!\n" + (isBlueTeamWin ? "Blue" : "Red") + " team win!", chatId);
        }

        private void startGame(Game game) {
                games.put(game.getChatId(), game);
                sendBoard("Game has started! Black team starts!\n@" + game.getCurrentCap() + " please start!", game.getChatId());
        }

        private void sendBoard(String text, long chatId) {
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                List<InlineKeyboardButton> buttons1 = new ArrayList<>();
                buttons1.add(new InlineKeyboardButton().setText("Show My Words").setCallbackData("CALLBACK_SHOW_WORDS"));

                List<InlineKeyboardButton> buttons2 = new ArrayList<>();
                buttons2.add(new InlineKeyboardButton().setText("Show Current Prompt").setCallbackData("CALLBACK_SHOW_PROMPT"));

                List<InlineKeyboardButton> buttons3 = new ArrayList<>();
                buttons3.add(new InlineKeyboardButton().setText("Show Code").setCallbackData("CALLBACK_SHOW_CODE"));

                buttons.add(buttons1);
                buttons.add(buttons2);
                buttons.add(buttons3);

                InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
                markupKeyboard.setKeyboard(buttons);

                SendMessage message = new SendMessage().setChatId(chatId).setText(text).setReplyMarkup(markupKeyboard);
                try {
                        execute(message);
                } catch (TelegramApiException e) {
                        e.printStackTrace();
                }
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
                sendSimpleMessage("START_INSTRUCTION", chatId);
        }

        private void sendSimpleMessage(String text, long chatId) {
                System.out.println("send to: " + chatId);
                SendMessage message = new SendMessage().setChatId(chatId).setText(text);
                try {
                        execute(message);
                } catch (TelegramApiException e) {
                        e.printStackTrace();
                }
        }

        @Override
        public String getBotUsername() {
                return "decryptorobot";
        }

        @Override
        public String getBotToken() {
                return "1411978821:AAH-u4ExtXwF04GFoZEzT4Ey3mzAUdHVihM";
        }
}