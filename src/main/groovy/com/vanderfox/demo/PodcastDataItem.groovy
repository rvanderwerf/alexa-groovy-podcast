package com.vanderfox.demo

import com.amazonaws.services.dynamodbv2.datamodeling.*
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Model representing an item of the ScoreKeeperUserData table in DynamoDB for the ScoreKeeper
 * skill.
 */
@DynamoDBTable(tableName = "HeroQuiz")
public class PodcastDataItem {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    private static final Logger log = LoggerFactory.getLogger(PodcastDataItem.class)

    private String token

    private long createdDate
    // Strings we will get are "streamUrl" and "podcastNumber"

    @DynamoDBHashKey(attributeName = "token")
    public int getToken() {
        log.info ("get item index is:  " + token)
        token
    }

    public void setToken(String token) {
        this.token = token;
        log.info ("set item token ex is:  " + token)
    }

    @DynamoDBAttribute(attributeName = "createdDate")
    public long getCreatedDate() {
        return createdDate
    }

    public void setCreatedDate(long createDate) {
        this.createdDate

    }
}
