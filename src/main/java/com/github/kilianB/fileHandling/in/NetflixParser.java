package com.github.kilianB.fileHandling.in;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.kilianB.model.netflix.NetflixMovie;
import com.github.kilianB.model.netflix.NetflixShowEpisode;
import com.github.kilianB.model.netflix.ViewItem;

/**
 * Attempt to extract the names of series and episode information of netflix titles.
 * Sadly the syntax isn't consistent. e.g. double quotes ... The current pattern might 
 * fail if colons are present in the episode title.
 * 
 * Sample of shows
 * 	"Star Trek: Discovery: Season 1: The Vulcan Hello"
 * 	"Breaking Bad: Season 1: Pilot"
 * 	"PussyTerror TV: Staffel 2: PussyTerror TV vom 16.04.2016"
 * "Haus des Geldes: Part 1: Episode 1"
 * 
 * @author Kilian
 *
 */
public class NetflixParser {
	
	/**
	 * Use the default logger for now
	 */
	private static final Logger LOGGER = Logger.getLogger(NetflixParser.class.getName());

	/**
	 * Input delimiter used in the CSV file
	 */
	private static final String INPUT_DELIMITER = ",";
	
	/** 
	 * We can't simply split line after line of the csv by delimiter since movie titles may also include commas.
	 * Sample : 
	 * 			"Narcos: Season 3: MRO","05/08/2018" And
	 * 			"13 Reasons Why: Season 1: Tape 7, Side A","18/05/2018"
	 */
	private final static Pattern splitLine = Pattern.compile("\"(?<Title>.*)\""+INPUT_DELIMITER+"\"(?<Date>.*)\"");
	
	/**
	 * If we have a show try to separate the series, season and episode title
	 */
	private final static Pattern showPattern = Pattern.compile(
			"(?<series>.*(?:(?:Season|Staffel|Part) [0-9]+)(?=:)): (?<epTitle>.*)",
			java.util.regex.Pattern.UNICODE_CHARACTER_CLASS);
	
	/**
	 * Once we get the season extract the season number
	 * TODO greater 10 + instead of *? But what if season does not include a number?
	 */
	private final static Pattern seasonPattern = Pattern.compile(
			"(?<series>.*(?=:)): (?<seasonText>[^0-9]*)(?<season>[0-9]*)",
			java.util.regex.Pattern.UNICODE_CHARACTER_CLASS);

	
	/**
	 * Hide default constructor
	 * 
	 * @param filePath
	 */
	@SuppressWarnings("unused")
	private NetflixParser(String filePath) {}

	public static ArrayList<ViewItem> parseHistoryFile(String filePath) throws IOException {

		Objects.requireNonNull(filePath);

		File viewFile = new File(filePath);

		if (!viewFile.exists()) {
			throw new IllegalArgumentException("Abort: Can not read Netflix view history file.");
		}

		var viewHistory = new ArrayList<ViewItem>();

		try (BufferedReader br = new BufferedReader(new FileReader(viewFile))) {

			// Skip csv header
			String line = br.readLine();

			// Maybe read everything into memory to work with stream api?
			while ((line = br.readLine()) != null) {
				//Tokenize 
				Matcher m = splitLine.matcher(line);
				if(m.find()) {
					
					var parsedEntry = parseEntry(m.group("Title"),m.group("Date"));
					viewHistory.add(parsedEntry);
				}else {
					LOGGER.warning("Line in the csv file can not be tokenized. Skipping line: " + line);
				}
			}
			return viewHistory;
		}
	}

	/**
	 * Keep it package private so we can unit test it
	 * 
	 * @return
	 */
	static ViewItem parseEntry(String title,String date) {

		Matcher m = showPattern.matcher(title);

		/*
		 * Input: "Star Trek: Discovery: Season 1: The Vulcan Hello"
		 * Capturing group
		 * 	series: Star Trek: Discovery: Season 1
		 * 	epTitle: The Vulcan Hello
		 */

		boolean matchFound = m.find();
		if (matchFound) {

			String seriesRaw = m.group("series");
			String episodeTitle = m.group("epTitle");

			Matcher titleMatcher = seasonPattern.matcher(seriesRaw);
			/*
			 * Input: Star Trek: Discovery: Season 1"
			 * Capturing group
			 * 	series: Star Trek: Discovery
			 * 	seasonText: Season
			 *	season: 1
			 */

			if (titleMatcher.find()) {

				String series = titleMatcher.group("series");

				/*Numerical number of the season.
				* If season is not populated this will contain the series name. e.g. if series
				* are not numbered.
				* TODO since show patterns quantifier was changed to + instead of * regarding the season number 
				* 	seasonText should never be the only thing populated and therefore gets discarded */
				//String season = titleMatcher.group("season") != null ? titleMatcher.group("season") : titleMatcher.group("seasonText");
			
				int season = Integer.parseInt(titleMatcher.group("season"));
				
				//System.out.println("Show:" + tokens[0] + " Token " + tokens[1]);
				
				return new NetflixShowEpisode(episodeTitle, date, season, series);
			} else {

				// Most likely a movie with a colon in the title
				return new NetflixMovie(title, date);
			}
		} else {
			// Most likely a movie
			return new NetflixMovie(title, date);
		}
	}

	public NetflixParser(String fileName, String delimiter) throws IOException {
		
		File csvFile = new File(fileName);
		
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))){
	
		}
		
	}
	
	
	
}
