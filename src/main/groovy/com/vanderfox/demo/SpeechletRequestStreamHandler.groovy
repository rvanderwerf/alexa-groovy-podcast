package com.vanderfox.demo

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Ryan Vanderwerf and Lee Fox on 3/18/16.
 */
/**
 * This class could be the handler for an AWS Lambda function powering an Alexa Skills Kit
 * experience. To do this, simply set the handler field in the AWS Lambda console to
 * "com.vanderfox.demo.SpeechletRequestStreamHandler" For this to work, you'll also need to build
 * this project using the {@code lambda-compile} Ant task and upload the resulting zip file to power
 * your function.
 */
public final class SpeechletRequestStreamHandler extends com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler {
    private  static final Logger log = LoggerFactory.getLogger(SpeechletRequestStreamHandler.class)
    private static final Set<String> supportedApplicationIds = new HashSet<String>()
    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        final Properties properties = new Properties();
        try {
            InputStream stream = GroovyPodcastSpeechlet.class.getClassLoader()getResourceAsStream("speechlet.properties")
            properties.load(stream);

            def property = properties.getProperty("awsApplicationId")
            log.info("Loading app ids: "+property)
            def appIds = property.split(",")
            appIds.each { appId ->
                log.info("loading app id "+appId)
                supportedApplicationIds.add(appId)
            }

        } catch (e) {
            log.error("Unable to aws application id. Please set up a springSocial.properties")
        }

    }


    public SpeechletRequestStreamHandler() {
        super(new GroovyPodcastSpeechlet(), supportedApplicationIds);
    }


}
