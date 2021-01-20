package com.bope.db;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UsersListMongo extends MongoRepository<UserMongo, String> {

    UserMongo findByUserName(String userName);
}
