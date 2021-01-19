package com.bope;
import com.bope.db.UserMongo;
import com.bope.db.UsersListMongo;
import com.bope.db.WordMongo;
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
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
public class DecryptoBot extends TelegramLongPollingBot {

        @Autowired
        private UsersListMongo usersListMongo;
        protected final Map<Long, Game> games = new HashMap<>();
        private static final Logger LOG = LoggerFactory.getLogger(DecryptoBot.class);



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
                                        if (callbackQuery.getData().equals("CALLBACK_SHOW_WORDS"))
                                                answerCallbackQuery(callbackQuery.getId(), game.getWordsListText(game.isPlayerBlue(userName)));
                                }

                        }

                        return;
                }



                Message message = update.getMessage();
                if (message == null)
                        return;
                chatId = message.getChatId();
                String text = message.getText();
                User user = message.getFrom();


                System.out.println(chatId + ": " + text);


                if (text.startsWith("/"))
                        text = text.substring(1);



                if (text.equals("start")) {
                        botStartCommand(chatId, user);
                        return;
                }



                if (text.startsWith("start")) {
                        text = text.substring(7);

                        Set<UserMongo> blueTeam = new HashSet<>();
                        Set<UserMongo> redTeam = new HashSet<>();
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
                                                        blueTeam.add(userMongo);
                                                } else {
                                                        redTeam.add(userMongo);
                                                }
                                        } else {
                                                unregisteredUsers.add(player);
                                        }
                                }
                                isFirstLine = false;
                        }

                        System.out.println("first:");
                        for (UserMongo u : blueTeam)
                                System.out.print(u.getUserName() + " ");
                        System.out.println("\nsecond:");
                        for (UserMongo u : redTeam)
                                System.out.print(u.getUserName() + " ");


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

                        for (UserMongo userMongo : blueTeam) {
                                if (redTeam.contains(userMongo)) {
                                        sendSimpleMessage("Every player should be presents in one team only!", chatId);
                                        return;
                                }
                        }
                        startGame(new Game(chatId, blueTeam, redTeam));
                }

        }



        private ArrayList<String> parsePrompts(String text) {
                String[] arr = text.split("\n");
                if (arr.length != 3)
                        return null;
                return new ArrayList<>(Arrays.asList(arr));
        }


        private void startGame(Game game) {
                games.put(game.getChatId(), game);
                UserMongo userFirst = game.getCurrentCap();
                sendBoard("Game has started! Black team starts!\n@" + userFirst.getUserName() + " please start!", game.getChatId());
        }


        private void sendBoard(String text, long chatId) {
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                List<InlineKeyboardButton> buttons0 = new ArrayList<>();
                buttons0.add(new InlineKeyboardButton().setText("Show My Words").setCallbackData("CALLBACK_SHOW_WORDS"));

                List<InlineKeyboardButton> buttons1 = new ArrayList<>();
                buttons1.add(new InlineKeyboardButton().setText("1").setCallbackData("CALLBACK DATA 1"));
                buttons1.add(new InlineKeyboardButton().setText("2").setCallbackData("CALLBACK DATA 2"));
                buttons1.add(new InlineKeyboardButton().setText("3").setCallbackData("CALLBACK DATA 3"));
                buttons1.add(new InlineKeyboardButton().setText("4").setCallbackData("CALLBACK DATA 4"));

                List<InlineKeyboardButton> buttons2 = new ArrayList<>();
                buttons2.add(new InlineKeyboardButton().setText("Show Code").setCallbackData("CALLBACK_SHOW_CODE"));

                buttons.add(buttons0);
                buttons.add(buttons1);
                buttons.add(buttons2);

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

        private void botStartCommand(long chatId, User user) {
                if (chatId == user.getId() && usersListMongo.findByUserName(user.getUserName()) == null) {
                        usersListMongo.save(new UserMongo(user.getUserName(), String.valueOf(user.getId())));
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