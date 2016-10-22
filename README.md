What is lazytest1
--------------------------------------

You've just created a basic Alexa Skills Lamba Groovy application. It provides a standard project structure and
a basic Gradle plugin. Now is up to you to add your source files and edit them with your favorite editor
and have fun :)

To build your app:
gradlew build

To deploy your app, set up your AWS credentials in ~/.aws/credentials file like so:

[vanderfox]
aws_access_key_id = ACCESS_KEY
aws_secret_access_key = SECRET_KEY

Now to deploy:

gradlew deploy


For more detailed directions on getting a skill working see:
https://github.com/rvanderwerf/alexa-twitter-groovy
