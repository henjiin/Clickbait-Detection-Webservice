# Clickbait Detection Webservice
## Webservice Basic Function
This Webservice tries to detect clickbait in given texts or tweets.
It's a Java Servlet, to be deployed on a Tomcat server.
To do so, the servlet trains classfiers on given datasets.
Upon GET Request,  the text or tweet is cast to a message class object, which gets classified by the specified model.
Finally, the servlet sends the detected class to the requester.

## Demo
Included is a demo website, which lets the user specify of on 20 given news publishers.
The webservice will classify the last 20 tweets of the user, which will get displayed.
The tweets will be color-coded, depending on the clickbait class.
This uses bootstrap and masonry for a tile layout.

