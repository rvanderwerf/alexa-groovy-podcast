package com.vanderfox.demo

import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.Context
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
import com.amazon.speech.ui.AudioDirectiveStop
import com.amazon.speech.ui.AudioItem
import com.amazon.speech.ui.PlainTextOutputSpeech
import com.amazon.speech.ui.Reprompt
import com.amazon.speech.ui.SimpleCard
import com.amazon.speech.ui.SsmlOutputSpeech
import com.amazon.speech.ui.Stream
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.slf4j.Logger;
import org.slf4j.LoggerFactory

/**
 * This app shows how to connect to hero with Spring Social, Groovy, and Alexa.
 * @author Lee Fox and Ryan Vanderwerf
 */
@CompileStatic
public class DemoSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(DemoSpeechlet.class);
    String title = "Demo Skill"

    @Override
    SpeechletResponse onPlaybackStarted(PlaybackStartedRequest playbackStartedRequest, Context context) throws SpeechletException {

        // don't do anything must return at least an empty list
        SpeechletResponse.newTellResponse([] as List<AudioDirective>)
    }

    @Override
    SpeechletResponse onPlaybackFinished(PlaybackFinishedRequest playbackFinishedRequest, Context context) throws SpeechletException {

        SpeechletResponse.newTellResponse([] as List<AudioDirective>)
    }

    @Override
    void onPlaybackStopped(PlaybackStoppedRequest playbackStoppedRequest, Context context) throws SpeechletException {
        // cannot return anything here
    }

    @Override
    SpeechletResponse onPlaybackNearlyFinished(PlaybackNearlyFinishedRequest playbackNearlyFinishedRequest, Context context) throws SpeechletException {

        // do nothing
        SpeechletResponse.newTellResponse([] as List<AudioDirective>)
    }

    @Override
    SpeechletResponse onPlaybackFailed(PlaybackFailedRequest playbackFailedRequest, Context context) throws SpeechletException {
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
    public SpeechletResponse onIntent(final IntentRequest request, final Session session, Context context)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;
        log.debug("incoming intent:${intentName}")

        switch (intentName) {
            case "PlayEpisodeIntent":
				playEpisode(request,session, context)
				break
			case "AMAZON.ResumeIntent":
                  resumeEpisode(request,session, context)
                  break

            case "AMAZON.HelpIntent":
			case "HelpIntent":
                getHelpResponse()
				break
			case "AMAZON.StopIntent":
			case "AMAZON.CancelIntent":
			    stopOrCancelPlayback()
				break
			case "AMAZON.PauseIntent":
				pausePlayback(session,request,context)
				break
			default:
                didNotUnderstand()
                break
        }
    }


	@CompileStatic(TypeCheckingMode.SKIP) // do some meta stuff
    public SpeechletResponse playEpisode(IntentRequest request, Session session, Context context) {


		log.debug("context:${context}")
		log.debug("context.audioPlayer.playerActivity:${context?.audioPlayer?.playerActivity}")
		log.debug("context.system.application.applicationId:${context?.system?.application?.applicationId}")
        Slot episodeNumber = request.intent.getSlot("podcastNumber")

        log.debug("episodeNumber:"+episodeNumber.value)

		session.setAttribute("podcastNumber",episodeNumber.value)
        String speechText = "Starting playback of Groovy Podcast Episode ${episodeNumber?.value}"
        // Create the Simple card content.
        SimpleCard card = new SimpleCard()
        card.setTitle(title)
        card.setContent(speechText) //TODO auto retrieve show notes here


		String streamUrl = ""
		def rssFeed = "https://groovypodcast.podbean.com/feed/".toURL().text

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
		session.setAttribute("streamUrl",streamUrl)

		if (streamUrl && streamUrl.size() > 0) {
			Stream audioStream = new Stream()
			audioStream.url = streamUrl
			audioStream.setToken((request.getRequestId()+streamUrl).hashCode() as String)
			audioStream.offsetInMilliseconds = 0
			//TODO update offset when we revieve a pause event
			AudioItem audioItem = new AudioItem(audioStream)


			AudioDirectivePlay audioPlayerPlay = new AudioDirectivePlay(audioItem)

			// write these to the dyanmo table to pause/resume will work (only way I've found)
			AmazonDynamoDBClient amazonDynamoDBClient
			amazonDynamoDBClient = new AmazonDynamoDBClient()
			DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient)

			Table table = dynamoDB.getTable("podcast_playback_state")
			Item tokenItem = new Item().withPrimaryKey("token",audioStream.getToken())
			.withString("streamUrl",audioStream.url)
			.withString("podcastNumber",episodeNumber.value)
			.withNumber("offsetInMillis",0)
			.withNumber("createdDate",System.currentTimeMillis())

			table.putItem(tokenItem)

			// Create the plain text output.
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
			speech.setText(speechText)


			SpeechletResponse.newTellResponse(speech, card, [audioPlayerPlay] as List<AudioDirective>)
		} else {
			def s = "I'm sorry I can't find that podcast"
			card.content = s
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
			speech.setText(s);
			SpeechletResponse.newTellResponse(speech, card)
		}

    }

	@CompileStatic(TypeCheckingMode.SKIP) // do some meta stuff
	public SpeechletResponse resumeEpisode(IntentRequest request, Session session, Context context) {



		log.debug("context:${context}")
		log.debug("context.audioPlayer.playerActivity:${context?.audioPlayer?.playerActivity}")
		log.debug("context.audioPlayer.token:${context?.audioPlayer?.token}")




		AmazonDynamoDBClient amazonDynamoDBClient
		amazonDynamoDBClient = new AmazonDynamoDBClient()
		DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient)

		Table table = dynamoDB.getTable("podcast_playback_state")
		Item item = table.getItem(new PrimaryKey("token", context?.audioPlayer?.token))
		if (item) {
			String speechText = "Resuming playback of Groovy Podcast Episode ${item.getString("podcastNumber")}"
			Stream audioStream = new Stream()
			audioStream.offsetInMilliseconds = 0

			audioStream.url = item.getString("streamUrl")
			audioStream.setToken(item.getString("token"))
			audioStream.offsetInMilliseconds = item.getNumber("offsetInMillis")
			AudioItem audioItem = new AudioItem(audioStream)


			AudioDirectivePlay audioPlayerPlay = new AudioDirectivePlay(audioItem)
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
			speech.setText(speechText)
			// Create the Simple card content.

			SimpleCard card = new SimpleCard()
			card.setTitle(title)
			card.setContent(speechText) //TODO auto retrieve show notes here
			SpeechletResponse.newTellResponse(speech, card, [audioPlayerPlay] as List<AudioDirective>)
		} else {
			String speechText = "I'm sorry I am unable to find your session to resume. Please say Alexa open Groovy Podcast and start over."
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
			speech.setText(speechText)
			// Create the Simple card content.
			SimpleCard card = new SimpleCard()
			card.setTitle(title)
			card.setContent(speechText) //TODO auto retrieve show notes here
			SpeechletResponse.newTellResponse(speech, card)
		}



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
        String speechText = "Welcome to The More Groovy Podcast Skill. To start playing a podcast say Play episode number"

        //askResponseFancy(speechText, speechText, "https://s3.amazonaws.com/vanderfox-sounds/groovybaby1.mp3")
		askResponse(speechText, speechText)

    }


    private SpeechletResponse askResponse(String cardText, String speechText) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard()
        card.setTitle(title)
        card.setContent(cardText)

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
        speech.setText(speechText)

        // Create reprompt
        Reprompt reprompt = new Reprompt()
        reprompt.setOutputSpeech(speech)

        SpeechletResponse.newAskResponse(speech, reprompt, card)
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
        String speechText = "Say play episode number to play an episode.";

        askResponse(speechText, speechText)
    }

	private SpeechletResponse stopOrCancelPlayback() {
		AudioDirectiveStop audioDirectiveClearQueue = new AudioDirectiveStop()
		//audioDirectiveClearQueue.clearBehaviour = "CLEAR_ALL"
		String speechText = "Stopping. GoodBye."
		// Create the Simple card content.
		SimpleCard card = new SimpleCard()
		card.setTitle(title)
		card.setContent(speechText)

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
		speech.setText(speechText)

		// Create reprompt
		Reprompt reprompt = new Reprompt()
		reprompt.setOutputSpeech(speech)

        log.debug("Stopping intent")

		SpeechletResponse.newTellResponse(speech,card,[audioDirectiveClearQueue] as List<AudioDirective>)
	}

	private SpeechletResponse pausePlayback(Session session, IntentRequest request, Context context) {



		log.debug("context:${context}")
		log.debug("context.audioPlayer.playerActivity:${context?.audioPlayer?.playerActivity}")
		log.debug("context.audioPlayer.token:${context?.audioPlayer?.token}")




		AmazonDynamoDBClient amazonDynamoDBClient
		amazonDynamoDBClient = new AmazonDynamoDBClient()
		DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient)

		Table table = dynamoDB.getTable("podcast_playback_state")
		try {
			Item item = table.getItem(new PrimaryKey("token", context?.audioPlayer?.token))

			if (item) {
				// update the offset where they paused at
				Item tokenItem = new Item().withPrimaryKey("token", item.getString("token"))
						.withString("streamUrl", item.getString("streamUrl"))
						.withString("podcastNumber", item.getString("podcastNumber"))
						.withNumber("offsetInMillis", context.audioPlayer.offsetInMilliseconds)
						.withNumber("createdDate", item.getNumber("createdDate"))
				table.deleteItem(new PrimaryKey("token",item.getString("token")))
				table.putItem(tokenItem)
				log.debug("found item: ${item.getString("podcastNumber")}")
			}
		} catch (Exception e) {
			log.debug("Error getting item from dynamo db token:${context?.audioPlayer?.token}")
		}

		AudioDirectiveStop audioDirectiveClearQueue = new AudioDirectiveStop()
		//audioDirectiveClearQueue.clearBehaviour = "CLEAR_ALL"
		String speechText = "Pausing playback. Say resume to restart playback."
		// Create the Simple card content.
		SimpleCard card = new SimpleCard()
		card.setTitle(title)
		card.setContent(speechText)

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
		speech.setText(speechText)

		// Create reprompt
		Reprompt reprompt = new Reprompt()
		reprompt.setOutputSpeech(speech)



		log.debug("Pausing intent")

		SpeechletResponse.newTellResponse(speech,card,[audioDirectiveClearQueue] as List<AudioDirective>)
	}


	private SpeechletResponse didNotUnderstand() {
        String speechText = "I'm sorry.  I didn't understand what you said.  Say play episode number to play an episode.";

        askResponse(speechText, speechText)
    }


/**
 * Initializes the instance components if needed.
 */
	private void initializeComponents(Session session) {
		AmazonDynamoDBClient amazonDynamoDBClient;
		amazonDynamoDBClient = new AmazonDynamoDBClient();
		/*ScanRequest req = new ScanRequest();
		req.setTableName("HeroQuiz");
		ScanResult result = amazonDynamoDBClient.scan(req)
		List quizItems = result.items
		int tableRowCount = quizItems.size()
		session.setAttribute("tableRowCount", Integer.toString(tableRowCount))
		log.info("This many rows in the table:  " + tableRowCount)*/
	}}
