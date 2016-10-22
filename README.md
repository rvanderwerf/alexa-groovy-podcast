Alexa Groovy Podcast
--------------------

This is an AWS Groovy Lambda Alexa skill for playing podcasts from the 'Groovy Podcast'. 
This demonstrates the community based java sdk https://github.com/vanderfox/alexa-skills-kit-java that had added Audio Directive support and other current pull requests and bug fixes.
To build your app:
gradlew build

To deploy your app, set up your AWS credentials in ~/.aws/credentials file like so:

[vanderfox]
aws_access_key_id = ACCESS_KEY
aws_secret_access_key = SECRET_KEY

Now to deploy:

gradlew deploy


For more detailed directions on getting a skill working see:
https://github.com/rvanderwerf/alexa-groovy-podcast


This skill was generated with Lazybones using the templates here: https://github.com/rvanderwerf/alexa-groovy-lazybones 