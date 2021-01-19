package com.bope.db;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "vocabulary")
public class WordMongo {

    @Id
    private ObjectId id;
    private String lang;
    private String word;

    public WordMongo(String word, String lang) {
        setWord(word);
        setLang(lang);
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }
}
