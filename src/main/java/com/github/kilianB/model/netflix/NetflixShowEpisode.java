package com.github.kilianB.model.netflix;

/**
 * Representing a single line found in the Netflix activity csv file
 *  most likely representing an episode of a show. 
 * @author Kilian
 *
 */
public class NetflixShowEpisode extends ViewItem{

	/**
	 * Season of this episode
	 */
	private int season; 	//int
	
	/**
	 * Title of the series
	 */
	private String series;	//Title of the show
	
	
	public NetflixShowEpisode(String title, String viewDate, int season, String series) {
		super(title, viewDate);
		this.season = season;
		this.series = series;
	}

	@Override
	public boolean isShow() {
		return true;
	}

	@Override
	public String toString() {
		return "NetflixShow [season=" + season + ", series=" + series + ", title=" + title + ", viewDate=" + viewDate + "]";
	}

	/**
	 * @return The season number as parsed from the viewing history file
	 * 	<p> Example: 1
	 */
	public int getSeason() {
		return season;
	}

	public void setSeason(int season) {
		this.season = season;
	}

	/**
	 * @return The name of the series as parsed from the viewing history file. 
	 * 	<p> Example: (Game of Thrones)
	 * 
	 */
	public String getSeries() {
		return series;
	}

	public void setSeries(String series) {
		this.series = series;
	}


	
	
}
