# Netflix Viewing Activity Visualizer

Visualize your personal netflix statistics, a small sample project utilizing *new features* found in Java 8, 9 and 10. 

Before you ask, no, I did not accumulate all those hours and episodes on my own. While the viewing history of subaccounts is seperated some people tend to use the service utilizing my profile ;). 

If you create your own graphics and stumble upon a great looking color theme feel free to send me a message and I'll add a small collection of presets. 

## Sample Output

![netflixinfographics](https://user-images.githubusercontent.com/9025925/44119605-05d28f44-a01a-11e8-9e02-7d381b2d6ff7.png)



## Usage

### 0. Prerequisites

<ul>
	<li><a href="http://www.oracle.com/technetwork/java/javase/downloads/jre10-downloads-4417026.html">Java 10</a> Runtime Environment. 
	</br>Used to parse the viewing activity file and download additional information for movies and series.</li>
	<li><a href="https://www.r-project.org">R 3.5.1</a> and <a href="https://www.rstudio.com">RStudio</a> (optional) for visualization</li>
	<li>A Trakt account to gain access to a movie metadata database.
		<ol>
			<li>Create an account at <a href="https://trakt.tv/auth/signin">Trakt.tv</a></li>
			<li>Create an "App" to retrieve your api key. <a href="https://trakt.tv/oauth/applications/new">https://trakt.tv/oauth/applications/new</a>. All you need to do is choose a random name and put a redirect uri doesn't need to be valid (e.g. "https://localhost.de")</li>
			<li>Copy the <b>client id</b> and save it somewhere for later usage</li>
		</ol>	
	</li>
	<li>Download the <a href="distribution.zip">distribution.zip</a> archive and extract the files</li>
</ul>

Check your java version by opening the terminal and type `java -version`


### 1 Gather information regarding your viewing activity

First we need to retrieve the viewing history file from Netflix. Sadly Netflix offers only a very limited data set, namely the series/movie title as well as the date it was watched. To generate more interesting statistics additional 
information like the runtime, genre, actors ... are needed. The java program will query the trakt database and do it's best to collect whatever material it can get it's hands on.

#### Manually download viewing activity file

Go to <a href="https://www.netflix.com/viewingactivity">https://www.netflix.com/viewingactivity</a> scroll to the bottom and download your viewing activity file.

![download](https://user-images.githubusercontent.com/9025925/44120336-5832a7fe-a01c-11e8-9bad-c5010a6d1e2d.png)


Locate the NetflixAnalyzer.jar file and place the downloaded csv alongside the jar

Open the terminal and type

````Shell
cd PathToJarFile
java -jar NetflixAnalyzer.jar traktClientId 
`````

![howto0_censored](https://user-images.githubusercontent.com/9025925/44120472-bac779ee-a01c-11e8-8da6-4c5b2a6373bc.jpg)

__Hint:__ on windows you only need to type `cd` and drag and drop the .jar file in the terminal. This will copy and paste the file location automatically for you. Alternatively you can click the *url bar* of the explorer and copy paste the path. 

Click enter and after a minute 3 additional csv files will appear. Warnings are perfectly fine. 

![howto1](https://user-images.githubusercontent.com/9025925/44120546-fa10671e-a01c-11e8-9563-8953a3606257.png)

### Convert the data to an awesome looking infographic

Now R comes into play. Fire up R Studio and open the CreateInfographics.r file. (Open the R folder and double click the file).

__Important__: After the R file opened click `File -> Reopen with encoding` and choose __UTF-8__.
 
 Scroll down to the settings section (around line 43) and adjust the paths (optinally adjust the color settings). Now you are good to go. Select the entire code block and click run. `Ctrl + A -> Ctrl + Enter`
 
 ![rhowto](https://user-images.githubusercontent.com/9025925/44121016-989ee03a-a01e-11e8-8634-c4c16e6482c8.png)

After a few seconds the infographics should be generated. 

<p align=center><img src="http://via.placeholder.com/500x200/ffffff/000000?text=You%20Are%20Done"/></p>




## Compile yourself 


If you wish to modify the code go ahead and clone the repository 
`git clone https://github.com/KilianB/NetflixViewingActivityVisualizer.git`.
`mvn package` will run the tests compile classes and bundle the binaries in the distribution.zip. 


## Data Accuracy
The netflix viewing activity data can be described as minimalistic at best. Once you started watching an episode/movie it will appear in the history file. We have no way to distinguish if someone just peeked at an item or fully watched it therefor the runtime will be overestimated. On the flipside, if an episode/movie was watched a second time the first entry will be removed from the history file resulting in an underestimation. All you can do is to reguarily download your viewing activity file and merge it to get a better representation of the data.

Selenium + a daily batch job + h2database anyone? Maybe a great next weekend project. 

A small amount of items are misclassified (movie as a show, show as a movie), either due to the fact that Trakt is not aware of those shows or because the parser's regex isn't good enough. Netflix doesn't make it easy either. Movies may have multiple colons, quotation marks, series may have titles without season number etc. A range of examples can be found in the Unit test <a href="src/test/java/fileHandling/in/TestNetflixParser.java">TestNetflixParser.java</a>


### 2 Ideas to increase the retrieval rate:

1. Attemp to improve the parser (have a look at <a href="src/main/java/com/github/kilianB/fileHandling/in/NetflixParser.java">NetflixParser.java</a>)

So far I used a rather easy regex, be my guest and improve it:

````java
	private final static Pattern splitLine = Pattern.compile("\"(?<Title>.*)\""+INPUT_DELIMITER+"\"(?<Date>.*)\"");
	
	/**
	 * If we have a show try to separate the series, season and episode title
	 */
	private final static Pattern showPattern = Pattern.compile(
			"(?<series>.*(?:(?:Season|Staffel|Part) [0-9]+)(?=:)): (?<epTitle>.*)",
			java.util.regex.Pattern.UNICODE_CHARACTER_CLASS);
	
	/**
	 * Once we get the season extract the season number
	 */
	private final static Pattern seasonPattern = Pattern.compile(
			"(?<series>.*(?=:)): (?<seasonText>[^0-9]*)(?<season>[0-9]*)",
			java.util.regex.Pattern.UNICODE_CHARACTER_CLASS);
````

A catch. Some series don't have season numbers which this regex assumes they do!.

2. If Trakt does not return a result when querying for a movie/show try the opposite and see if we receive anything useful. 

Take a look at <a href="src/main/java/com/github/kilianB/launcher/NetflixAnalyzer.java">NetflixAnalyzer.java</a>


## Disclaimer 

The source was never intended to go public therefore no time was spend on the code being understandable or optimized. The main objective was to use some new concepts I haven't had much exposure to recently. 

The interesting stuff happens in the gigantic blob <a href="src/main/java/com/github/kilianB/launcher/NetflixAnalyzer.java">NetflixAnalyzer.java</a>


* Java 8 
	* Method reference :: 
	* Predicates
* Java 9: 
	* streams 
	* lambda expressions
* Java 10 
	*local variable type inference

I have no idea how to use R. Everything in the R file should not be considered good coding. I am aware of the glitches in `gridTextMulticolor`. I just wrote it to be good enough for the use cases I encountered. 


## License

The project is licensed under <a href="">GPLv3</a>.
Icons used were downloaded from <a href="https://www.flaticon.com">flaticon</a> and <a href="freepik.com">freepik</a> and are licensed
by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a>.
Individual authors : 
- <a href="https://www.flaticon.com/authors/alfredo-hernandez" title="Alfredo Hernandez">Alfredo Hernandez</a>
- <a href="https://www.flaticon.com/authors/smashicons" title="Smashicons">Smashicons</a>
- <a href="https://www.flaticon.com/authors/vectors-market" title="Vectors Market">Vectors Market</a>
- <a href="http://www.freepik.com" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a>
- <a href="https://www.flaticon.com/authors/ocha" title="OCHA">OCHA</a>
- <a href="https://www.flaticon.com/authors/alessio-atzeni" title="Alessio Atzeni">Alessio Atzeni</a>.


