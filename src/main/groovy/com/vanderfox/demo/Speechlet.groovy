package com.vanderfox.demo

import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.IntentRequest
import com.amazon.speech.speechlet.LaunchRequest
import com.amazon.speech.speechlet.PlaybackFailedRequest
import com.amazon.speech.speechlet.PlaybackFinishedRequest
import com.amazon.speech.speechlet.PlaybackNearlyFinishedRequest
import com.amazon.speech.speechlet.PlaybackStartedRequest
import com.amazon.speech.speechlet.PlaybackStoppedRequest
import com.amazon.speech.speechlet.Session
import com.amazon.speech.speechlet.SessionEndedRequest
import com.amazon.speech.speechlet.SessionStartedRequest
import com.amazon.speech.speechlet.Speechlet
import com.amazon.speech.speechlet.SpeechletException
import com.amazon.speech.speechlet.SpeechletResponse
import com.amazon.speech.ui.AudioDirective
import com.amazon.speech.ui.AudioDirectivePlay
import com.amazon.speech.ui.AudioItem
import com.amazon.speech.ui.PlainTextOutputSpeech
import com.amazon.speech.ui.Reprompt
import com.amazon.speech.ui.SimpleCard
import com.amazon.speech.ui.SsmlOutputSpeech
import com.amazon.speech.ui.Stream
import groovy.transform.CompileStatic
import org.slf4j.Logger;
import org.slf4j.LoggerFactory
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

/**
 * This app shows how to connect to hero with Spring Social, Groovy, and Alexa.
 * @author Lee Fox and Ryan Vanderwerf
 */
@CompileStatic
public class DemoSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(DemoSpeechlet.class);
    String title = "Demo Skill"

    @Override
    void onPlaybackStarted(PlaybackStartedRequest playbackStartedRequest) throws SpeechletException {

    }

    @Override
    void onPlaybackFinished(PlaybackFinishedRequest playbackFinishedRequest) throws SpeechletException {

    }

    @Override
    void onPlaybackStopped(PlaybackStoppedRequest playbackStoppedRequest) throws SpeechletException {

    }

    @Override
    void onPlaybackNearlyFinished(PlaybackNearlyFinishedRequest playbackNearlyFinishedRequest) throws SpeechletException {

    }

    @Override
    void onPlaybackFailed(PlaybackFailedRequest playbackFailedRequest) throws SpeechletException {

    }

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId())


        session.setAttribute("something", "a session value")

        initializeComponents(session)

        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        getWelcomeResponse(session);
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;
      

        switch (intentName) {
            case "PlayEpisodeIntent":

                  playEpisode(request,session)
                  break

            case "EndGameIntent":
                endGame()
                break
            case "HelpIntent":
                getHelpResponse()
            default:
                didNotUnderstand()
                break
        }
    }


    public SpeechletResponse playEpisode(IntentRequest request, Session session) {


        Slot episodeNumber = request.intent.getSlot("podcastNumber")

        log.debug("episodeNumber:"+episodeNumber)

        String speechText = "Starting playback of Groovy Podcast Episode ${episodeNumber?.value}"
        // Create the Simple card content.
        SimpleCard card = new SimpleCard()
        card.setTitle(title)
        card.setContent(speechText) //TODO auto retrieve show notes here




        Stream audioStream = new Stream()
        audioStream.offsetInMilliseconds = 0
        audioStream.url = "https://groovypodcast.podbean.com/mf/feed/8ic9x9/Groovy_Podcast_Ep_35.mp3"
        audioStream.setToken("http://groovypodcast.podbean.com/mf/feed/8ic9x9/Groovy_Podcast_Ep_35.mp3".hashCode() as String)
        AudioItem audioItem = new AudioItem(audioStream)

        AudioDirectivePlay audioPlayerPlay = new AudioDirectivePlay(audioItem)

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
        speech.setText(speechText);


        SpeechletResponse.newTellResponse(speech, card, [audioPlayerPlay] as List<AudioDirective>)

    }
    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any cleanup logic goes here
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse(final Session session) {
        String speechText = "Welcome to Groovy Podcast Skill. To start playing a podcast say Play episode number"

        askResponseFancy(speechText, speechText, "https://s3.amazonaws.com/vanderfox-sounds/test.mp3")

    }


    private SpeechletResponse askResponse(String cardText, String speechText) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(cardText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    private SpeechletResponse tellResponse(String cardText, String speechText) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(cardText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        SpeechletResponse.newTellResponse(speech, card);
    }



    private SpeechletResponse askResponseFancy(String cardText, String speechText, String fileUrl) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard()
        card.setTitle(title)
        card.setContent(cardText)

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
        speech.setText(speechText);
        log.info("making welcome audio")
        SsmlOutputSpeech fancySpeech = new SsmlOutputSpeech()
        fancySpeech.ssml = "<speak><audio src=\""+fileUrl+"\"/> "+speechText+"</speak>"
        log.info("finished welcome audio")
        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(fancySpeech);

        SpeechletResponse.newAskResponse(fancySpeech, reprompt, card)
    }



    /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "Say Exit Game or Quit Game to stop the game.  Please follow the prompts I give you, and be sure to speak clearly.";

        askResponse(speechText, speechText)
    }

    private SpeechletResponse didNotUnderstand() {
        String speechText = "I'm sorry.  I didn't understand what you said.  Say help me for help.";

        askResponse(speechText, speechText)
    }

    private SpeechletResponse endGame() {
        String speechText = "OK.  I will stop the game now.  Please try again soon.";

        tellResponse(speechText, speechText)
    }


    /**
     * Initializes the instance components if needed.
     */
    private void initializeComponents(Session session) {
        // initialize any components here like set up a dynamodb connection
    }
}
