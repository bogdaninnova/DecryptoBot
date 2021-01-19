package com.bope.db;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface WordsListMongo extends MongoRepository<WordMongo, String> {

    List<WordMongo> findByLang(String lang);

}
