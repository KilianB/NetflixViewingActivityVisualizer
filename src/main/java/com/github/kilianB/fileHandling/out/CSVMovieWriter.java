package com.github.kilianB.fileHandling.out;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.github.kilianB.model.netflix.NetflixMovie;
import com.uwetrottmann.trakt5.entities.Movie;

/**
 * Rudimentary synchronized CSV writer accepting movie objects to be written to a CSV file 
 * 
 * @author Kilian
 */
public class CSVMovieWriter extends CSVWriter{
	
	public CSVMovieWriter(File csvOutPath, String delimiter) throws IOException {
		super(csvOutPath, delimiter,"Date","Title","Runtime","Certificate","Released","Genres");
	}
	
	/**
	 * Append the data to the end of the csv file in a synchronized manner.
	 * @param movie Movie object retrieved from Trakt
	 * @param netflixObj matching movie item parsed from the viewinghistory file
	 * @throws IOException if an IOError occurs
	 */
	public void push(Movie movie, NetflixMovie netflixObj) throws IOException {
		
		String genres = "";
		if(movie.genres != null) {
			genres = Arrays.toString(movie.genres.toArray(new String[movie.genres.size()]));
		}
		
		writeLine(netflixObj.getViewDate(),
				movie.title,
				movie.runtime,
				movie.certification,
				movie.released,
				genres
				);
		}
}
