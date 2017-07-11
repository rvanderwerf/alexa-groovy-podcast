package com.vanderfox.demo

import com.amazon.speech.json.SpeechletRequestEnvelope
import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.Context
import com.amazon.speech.speechlet.Directive
import com.amazon.speech.speechlet.IntentRequest
import com.amazon.speech.speechlet.LaunchRequest

import com.amazon.speech.speechlet.Session
import com.amazon.speech.speechlet.SessionEndedRequest
import com.amazon.speech.speechlet.SessionStartedRequest
import com.amazon.speech.speechlet.Speechlet
import com.amazon.speech.speechlet.SpeechletException
import com.amazon.speech.speechlet.SpeechletResponse
import com.amazon.speech.speechlet.SpeechletV2
import com.amazon.speech.speechlet.SupportedInterfaces
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioItem
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayer
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayerInterface
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayerState
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.ClearQueueDirective
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.StopDirective
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackFailedRequest
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackFinishedRequest
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackNearlyFinishedRequest
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackStartedRequest
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackStoppedRequest
import com.amazon.speech.speechlet.interfaces.display.directive.RenderTemplateDirective
import com.amazon.speech.speechlet.interfaces.display.element.ImageInstance
import com.amazon.speech.speechlet.interfaces.display.element.RichText
import com.amazon.speech.speechlet.interfaces.display.template.BodyTemplate1
import com.amazon.speech.speechlet.interfaces.system.SystemInterface
import com.amazon.speech.speechlet.interfaces.system.SystemState
import com.amazon.speech.ui.Card
import com.amazon.speech.ui.OutputSpeech
import com.amazon.speech.ui.PlainTextOutputSpeech
import com.amazon.speech.ui.Reprompt
import com.amazon.speech.ui.SimpleCard
import com.amazon.speech.ui.SsmlOutputSpeech
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.document.Table
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.slf4j.Logger;
import org.slf4j.LoggerFactory

/**
 * This app shows how to connect to hero with Spring Social, Groovy, and Alexa.
 * @author Lee Fox and Ryan Vanderwerf
 */
@CompileStatic
public class GroovyPodcastSpeechlet implements SpeechletV2, AudioPlayer {
    private static final Logger log = LoggerFactory.getLogger(GroovyPodcastSpeechlet.class);
    String title = "The Groovy Podcast Player Skill"





	@Override
	SpeechletResponse onPlaybackStarted(SpeechletRequestEnvelope<PlaybackStartedRequest> requestEnvelope) {

        // don't do anything must return at least an empty lis

        tellResponse("","",[] as ArrayList<Directive>,requestEnvelope.getContext())
    }

    @Override
	SpeechletResponse onPlaybackFinished(SpeechletRequestEnvelope<PlaybackFinishedRequest> requestEnvelope) {

		tellResponse("","",[] as ArrayList<Directive>,requestEnvelope.getContext())
    }

    @Override
	SpeechletResponse onPlaybackStopped(SpeechletRequestEnvelope<PlaybackStoppedRequest> requestEnvelope) {
        // cannot return anything here
    }

    @Override
	SpeechletResponse onPlaybackNearlyFinished(SpeechletRequestEnvelope<PlaybackNearlyFinishedRequest> requestEnvelope) {

        // do nothing
		tellResponse("","",[] as ArrayList<Directive>,requestEnvelope.getContext())
    }

    @Override
	SpeechletResponse onPlaybackFailed(SpeechletRequestEnvelope<PlaybackFailedRequest> requestEnvelope) {
        //clear queue as something is wrong
        ClearQueueDirective audioDirectiveClearQueue = new ClearQueueDirective()
		tellResponse("","",[audioDirectiveClearQueue] as ArrayList<Directive>,requestEnvelope.getContext())
    }


    @Override
	void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope)
			throws SpeechletException {
		Session session = requestEnvelope.getSession()
        log.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.request.getRequestId(),
                session.getSessionId())


        session.setAttribute("something", "a session value")

        initializeComponents(session)

        // any initialization logic goes here
    }

    @Override
	SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope)
			throws SpeechletException {

        log.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());

        getWelcomeResponse(requestEnvelope.session);
    }


	@Override
	SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {

		Session session = requestEnvelope.getSession()
		IntentRequest request = requestEnvelope.getRequest()
		Context context = requestEnvelope.getContext()
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;
        log.debug("incoming intent:${intentName}")

        switch (intentName) {
			case "PlayLatestEpisodeIntent":
				playLatestEpisode(request,session, context)
				break;
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
				pausePlayback(session,request,requestEnvelope)
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
		if (!episodeNumber?.value) {
			return didNotUnderstand()
		}

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
					card.setTitle((String)item.title.text())
					// we need to strip html tags out of the card because it is text only allowed
					String description = item.description.text()
					description = description.replaceAll("<.*?>", "")
					card.setContent(description)
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

			AudioItem audioItem = new AudioItem(audioStream)


			PlayDirective audioPlayerPlay = new PlayDirective(audioItem)

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


			tellResponse(card, speech, [audioPlayerPlay] as List<Directive>, context)
		} else {
			def s = "I'm sorry I can't find that podcast"
			card.content = s
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
			speech.setText(s);
			tellResponse(card, speech,[] as List<Directive>, context)
		}

    }


	@CompileStatic(TypeCheckingMode.SKIP) // do some meta stuff
	public SpeechletResponse playLatestEpisode(IntentRequest request, Session session, Context context) {


		log.debug("Playing latest episode context:${context}")
		log.debug("context.audioPlayer.playerActivity:${context?.audioPlayer?.playerActivity}")
		log.debug("context.system.application.applicationId:${context?.system?.application?.applicationId}")
		String episodeNumber = ""

		session.setAttribute("podcastNumber",episodeNumber.value)
		String speechText = "Starting playback of Latest Groovy Podcast Episode"
		// Create the Simple card content.
		SimpleCard card = new SimpleCard()
		card.setTitle(title)
		card.setContent(speechText) //TODO auto retrieve show notes here


		String streamUrl = ""
		def rssFeed = "https://groovypodcast.podbean.com/feed/".toURL().text

		def slurper = new XmlParser(false,false).parseText(rssFeed)
		if (slurper) {

			long lastEpisodeDate = 0
			slurper.channel.item.each { item ->
				//log.debug("item content:${item}")
				log.debug("found item:"+item.title.text())
				log.debug("found link:"+item.link.text())
				log.debug("found enclosure:"+item.enclosure)
				log.debug("found enclosure pubDate: ${item.pubDate.text()}")
				String pubDate = item.pubDate.text()
				long thisEpisodeDate = Date.parse("EEE, dd MMM yyyy HH:mm:ss ZZZZZ",pubDate).time

				if (thisEpisodeDate > lastEpisodeDate) {
					log.debug("found episode:${episodeNumber.value} streamurl:${item.enclosure.@url.value[0]}")
					streamUrl = item.enclosure.@url.value[0].toString()
					card.setTitle((String)item.title.text())
					String description = item.description.text()
					description = description.replaceAll("<.*?>", "")
					card.setContent(description)
					try {
						episodeNumber = item.title.text().findAll(/\d+/)*.toInteger()
					} catch (Exception) {
						//keep going if an episode is not parseable to find the number
						log.debug("skipping episode with title:${item.title.text} - no numbers found")
					}
				}
				lastEpisodeDate = thisEpisodeDate
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
			AudioItem audioItem = new AudioItem()
			audioItem.stream = audioStream

			PlayDirective audioPlayerPlay = new PlayDirective()
			audioPlayerPlay.audioItem = audioItem

			// write these to the dyanmo table to pause/resume will work (only way I've found)
			AmazonDynamoDBClient amazonDynamoDBClient
			amazonDynamoDBClient = new AmazonDynamoDBClient()
			DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient)

			Table table = dynamoDB.getTable("podcast_playback_state")
			Item tokenItem = new Item().withPrimaryKey("token",audioStream.getToken())
					.withString("streamUrl",audioStream.url)
					.withString("podcastNumber",episodeNumber)
					.withNumber("offsetInMillis",0)
					.withNumber("createdDate",System.currentTimeMillis())

			table.putItem(tokenItem)

			// Create the plain text output.
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
			speech.setText(speechText)


			tellResponse(speechText, speechText, [audioPlayerPlay] as List<Directive>, context)
		} else {
			def s = "I'm sorry I can't find that podcast"
			card.content = s
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
			speech.setText(s);
			tellResponse(speechText, speechText, [] as List<Directive>, context)
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
			audioStream.seoffsetInMilliseconds = item.getNumber("offsetInMillis")
			AudioItem audioItem = new AudioItem()
			audioItem.stream = audioStream


			PlayDirective audioPlayerPlay = new PlayDirective()
			audioPlayerPlay.audioItem = audioItem
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
			speech.setText(speechText)
			// Create the Simple card content.

			SimpleCard card = new SimpleCard()
			card.setTitle(title)
			card.setContent(speechText) //TODO auto retrieve show notes here
			tellResponse(card, speech, [audioPlayerPlay] as List<Directive>)
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
	void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {

        log.info("onSessionEnded requestId={}, sessionId={}", requestEnvelope.request.getRequestId(),
                requestEnvelope.session.getSessionId());

        // any cleanup logic goes here
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse(final Session session) {
        String speechText = "Welcome to The Groovy Podcast Skill. To start playing a podcast say 'Play episode number' or say 'Play latest episode'"

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
		StopDirective audioDirectiveClearQueue = new StopDirective()
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

		tellResponse(card,speech,[audioDirectiveClearQueue] as List<Directive>)
	}

	private SpeechletResponse pausePlayback(Session session, IntentRequest request, SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {



		log.debug("context:${requestEnvelope.context.toString()}")
		SystemState state = getSystemState(requestEnvelope.context)
		AudioPlayerState audioPlayerState = getAudioPlayerState(requestEnvelope.context)
		log.debug("context.audioPlayer.playerActivity:${audioPlayerState.playerActivity.name()}")
		log.debug("context.audioPlayer.token:${audioPlayerState.token}")




		AmazonDynamoDBClient amazonDynamoDBClient
		amazonDynamoDBClient = new AmazonDynamoDBClient()
		DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient)

		Table table = dynamoDB.getTable("podcast_playback_state")
		try {
			Item item = table.getItem(new PrimaryKey("token", audioPlayerState.token))

			if (item) {
				// update the offset where they paused at
				Item tokenItem = new Item().withPrimaryKey("token", item.getString("token"))
						.withString("streamUrl", item.getString("streamUrl"))
						.withString("podcastNumber", item.getString("podcastNumber"))
						.withNumber("offsetInMillis", audioPlayerState.offsetInMilliseconds)
						.withNumber("createdDate", item.getNumber("createdDate"))
				table.deleteItem(new PrimaryKey("token",item.getString("token")))
				table.putItem(tokenItem)
				log.debug("found item: ${item.getString("podcastNumber")}")
			}
		} catch (Exception e) {
			log.debug("Error getting item from dynamo db token:${audioPlayerState.token}")
		}

		StopDirective audioDirectiveClearQueue = new StopDirective()
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

		tellResponse(card,speech,[audioDirectiveClearQueue] as List<Directive>, requestEnvelope.context)
	}


	private SpeechletResponse didNotUnderstand() {
        String speechText = "I'm sorry.  I didn't understand what you said.  Say play episode number to play an episode or say play latest episode.";

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
	}

	private SystemState getSystemState(Context context) {
		return context.getState(SystemInterface.class, SystemState.class);
	}

	private AudioPlayerState getAudioPlayerState(Context context) {
		return context.getState(AudioPlayerInterface.class, AudioPlayerState.class);
	}

	private SpeechletResponse tellResponse(String cardText, String speechText, List<Directive> directives = [] as List<Directive>, Context context = null) {

		SimpleCard card = new SimpleCard()
		card.setContent(cardText)
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech()
		outputSpeech.setText(speechText)
		tellResponse(card,outputSpeech,directives,context)


	}

	private SpeechletResponse tellResponse(Card card, OutputSpeech outputSpeech, List<Directive> directives = [] as List<Directive>, Context context = null) {

		RenderTemplateDirective renderTemplateDirective = buildBodyTemplate1(card.toString())




		// Create reprompt
		Reprompt reprompt = new Reprompt()
		reprompt.setOutputSpeech(outputSpeech);

		SpeechletResponse response = new SpeechletResponse()
		SystemState systemState = getSystemState(context)
		SupportedInterfaces supportedInterfaces = systemState.device.getSupportedInterfaces()
		ArrayList<Directive> supportedDirectives = [] as ArrayList<Directive>
		for (Directive directive: directives) {
			if (supportedInterfaces.isInterfaceSupported(directive.class)) {
				supportedDirectives.add(directive)
			}
		}
		if (supportedDirectives.size() > 0) {
			response.setDirectives(supportedDirectives)
		}
		boolean hasDisplay = false
		response.setNullableShouldEndSession(true)
		response.setOutputSpeech(outputSpeech)
		response.setReprompt(reprompt)
		response


	}


	private RenderTemplateDirective buildBodyTemplate1(String cardText) {
		BodyTemplate1 template = new BodyTemplate1()
		template.setTitle("Groovy Podcast")
		BodyTemplate1.TextContent textContent = new BodyTemplate1.TextContent()
		RichText richText = new RichText()
		richText.text = cardText
		textContent.setPrimaryText(richText)
		template.setTextContent(textContent)
		com.amazon.speech.speechlet.interfaces.display.element.Image backgroundImage = new com.amazon.speech.speechlet.interfaces.display.element.Image()
		ImageInstance imageInstance = new ImageInstance()
		imageInstance.setUrl("https://s-media-cache-ak0.pinimg.com/originals/e4/30/78/e43078050e9a8d5bc2f8a1ed09a77227.png")
		ArrayList<ImageInstance> imageInstances = new ArrayList()
		imageInstances.add(imageInstance)
		backgroundImage.setSources(imageInstances)
		template.setBackgroundImage(backgroundImage)
		RenderTemplateDirective renderTemplateDirective = new RenderTemplateDirective()
		renderTemplateDirective.setTemplate(template)
		renderTemplateDirective
	}
}
