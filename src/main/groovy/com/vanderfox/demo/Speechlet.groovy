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
import com.amazon.speech.speechlet.SystemExceptionEncounteredRequest
import com.amazon.speech.ui.AudioDirective
import com.amazon.speech.ui.AudioDirectiveClearQueue
import com.amazon.speech.ui.AudioDirectivePlay
import com.amazon.speech.ui.AudioItem
import com.amazon.speech.ui.PlainTextOutputSpeech
import com.amazon.speech.ui.Reprompt
import com.amazon.speech.ui.SimpleCard
import com.amazon.speech.ui.SsmlOutputSpeech
import com.amazon.speech.ui.Stream
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.slf4j.Logger;
import org.slf4j.LoggerFactory

import java.util.regex.Pattern


/**
 * This app shows how to connect to hero with Spring Social, Groovy, and Alexa.
 * @author Lee Fox and Ryan Vanderwerf
 */
@CompileStatic
public class DemoSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(DemoSpeechlet.class);
    String title = "Demo Skill"

    @Override
    SpeechletResponse onPlaybackStarted(PlaybackStartedRequest playbackStartedRequest) throws SpeechletException {

        // don't do anything must return at least an empty list
        SpeechletResponse.newTellResponse([] as List<AudioDirective>)
    }

    @Override
    SpeechletResponse onPlaybackFinished(PlaybackFinishedRequest playbackFinishedRequest) throws SpeechletException {

        SpeechletResponse.newTellResponse([] as List<AudioDirective>)
    }

    @Override
    void onPlaybackStopped(PlaybackStoppedRequest playbackStoppedRequest) throws SpeechletException {
        // cannot return anything here
    }

    @Override
    SpeechletResponse onPlaybackNearlyFinished(PlaybackNearlyFinishedRequest playbackNearlyFinishedRequest) throws SpeechletException {

        // do nothing
        SpeechletResponse.newTellResponse([] as List<AudioDirective>)
    }

    @Override
    SpeechletResponse onPlaybackFailed(PlaybackFailedRequest playbackFailedRequest) throws SpeechletException {
        //clear queue as something is wrong
        AudioDirectiveClearQueue audioDirectiveClearQueue = new AudioDirectiveClearQueue()
        SpeechletResponse.newTellResponse([audioDirectiveClearQueue] as List<AudioDirective>)
    }

    @Override
    void onSystemException(SystemExceptionEncounteredRequest systemExceptionEncounteredRequest) throws SpeechletException {
        log.debug("error encountered: cause: ${systemExceptionEncounteredRequest.cause.requestId} error: ${systemExceptionEncounteredRequest.error.message}")
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


	@CompileStatic(TypeCheckingMode.SKIP) // do some meta stuff
    public SpeechletResponse playEpisode(IntentRequest request, Session session) {


        Slot episodeNumber = request.intent.getSlot("podcastNumber")

        log.debug("episodeNumber:"+episodeNumber.value)

        String speechText = "Starting playback of Groovy Podcast Episode ${episodeNumber?.value}"
        // Create the Simple card content.
        SimpleCard card = new SimpleCard()
        card.setTitle(title)
        card.setContent(speechText) //TODO auto retrieve show notes here


		String streamUrl = ""
		def rssFeed = "https://groovypodcast.podbean.com/feed/".toURL().text
		//log.debug("rss feed:${rssFeed}")
        def slurper = new XmlParser(false,false).parseText(rssFeed)
        if (slurper) {
			slurper.channel.item.each { item ->
				//log.debug("item content:${item}")
				log.debug("found item:"+item.title.text())
				log.debug("found link:"+item.link.text())
				log.debug("found enclosure:"+item.enclosure)
				log.debug("found enclosure url:"+item.enclosure.@url.value[0])
				if (item.title.text().indexOf(episodeNumber.value)!= -1) {
					log.debug("found episode:${episodeNumber.value} streamurl:${item.enclosure.@url.value[0]}")
					streamUrl = item.enclosure.@url.value[0].toString()
				}

			}
		}
		log.debug("streamUrl:${streamUrl}")
		// replace http with https or alexa won't play it
		streamUrl = streamUrl.replaceAll('http','https')
		log.debug("streamUrl replaced:${streamUrl}")


        Stream audioStream = new Stream()
        audioStream.offsetInMilliseconds = 0
        //audioStream.url = "https://groovypodcast.podbean.com/mf/feed/8ic9x9/Groovy_Podcast_Ep_35.mp3"
        audioStream.url = streamUrl
		audioStream.setToken(streamUrl.hashCode() as String)
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

        askResponseFancy(speechText, speechText, "https://s3.amazonaws.com/vanderfox-sounds/groovybaby1.mp3")

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
