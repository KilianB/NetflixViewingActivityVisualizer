package com.github.kilianB.fileHandling.out;

import java.io.File;
import java.io.IOException;
import org.threeten.bp.format.DateTimeFormatter;
import java.util.Arrays;

import com.github.kilianB.model.netflix.NetflixShowEpisode;
import com.uwetrottmann.trakt5.entities.Show;

/**
 * Rudimentary synchronized CSV writer accepting Show objects to be written to a CSV file 
 * 
 * @author Kilian
 */
public class CSVShowWriter extends CSVWriter{
	
	public CSVShowWriter(File csvOutPath, String delimiter) throws IOException {
		super(csvOutPath, delimiter,"Date","Series","Title","Season","Runtime","Certificate","FirstAired","Network","Genres");
	}
	
	/**
	 * Output date format of the first aired date
	 */
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	
	/**
	 * Append the data to the end of the csv file in a synchronized manner.
	 * @param show	A show object received by trakt
	 * @param netflixShow	A matching object parsed from the viewfile.csv
	 * @param runtime	The runtime of the episode
	 * @throws IOException if an IO Error occurs
	 */
	public void push(Show show, NetflixShowEpisode netflixShow, int runtime) throws IOException {
		
		String genres = "";
		if(show.genres != null) {
			genres = Arrays.toString(show.genres.toArray(new String[show.genres.size()]));
		}
		
		try {
			dtf.format(show.first_aired);
		}catch(IllegalArgumentException e) {
			System.out.println("First aired: " + show.first_aired);
		}
		
		writeLine(netflixShow.getViewDate(),
				netflixShow.getSeries(),
				netflixShow.getTitle(),
				netflixShow.getSeason(),
				runtime,
				show.certification,
				show.first_aired.format(dtf),
				show.network,
				genres);
	}
	
	
	
}
