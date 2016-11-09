package com.vanderfox.demo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeechletDynamoDbClient {
	private final AmazonDynamoDBClient dynamoDBClient;
	private static final Logger log = LoggerFactory.getLogger(SpeechletDynamoDbClient.class);

	public SpeechletDynamoDbClient(final AmazonDynamoDBClient dynamoDBClient) {
		this.dynamoDBClient = dynamoDBClient;
	}

	/**
	 * Loads an item from DynamoDB by primary Hash Key. Callers of this method should pass in an
	 * object which represents an item in the DynamoDB table item with the primary key populated.
	 *
	 * @param tableItem
	 * @return
	 */
	public PodcastDataItem loadItem(final PodcastDataItem tableItem) {
		DynamoDBMapper mapper = createDynamoDBMapper();
		log.info("getting ready to load the item")
		PodcastDataItem item = mapper.load(tableItem);
		log.info("Returned item:  " + item)
		item;
	}

	/**
	 * Creates a {@link DynamoDBMapper} using the default configurations.
	 *
	 * @return
	 */
	private DynamoDBMapper createDynamoDBMapper() {
		new DynamoDBMapper(dynamoDBClient);
	}
}
