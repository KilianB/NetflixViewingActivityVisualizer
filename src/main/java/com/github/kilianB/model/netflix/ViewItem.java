package com.github.kilianB.model.netflix;

/**
 * Basic Item representing a single line found in the Netflix activity csv file
 * @author Kilian
 *
 */
public abstract class ViewItem {

	//Data read by parsing the input file. 
	
	/**
	 * Title of the movie or show
	 */
	protected String title;
	/**
	 * Date the episode/movie was watched on netflix. Hence no data manipulation takes place in java
	 * no reason to cast it into an actual date object
	 */
	protected String viewDate;
	
	
	public ViewItem(String title, String viewDate) {
		this.title = title;
		this.viewDate = viewDate;
	}

	/**
	 * Return true if this object represents a netflix show/episode.
	 * Return false if it is a movie.
	 * TODO bad OOP style. ... instanceof could be used as well. Anyways
	 * go with it for now.
	 * @return
	 */
	public abstract boolean isShow();
	
	/**
	 * @return The title of the movie or show as parsed directly from the netflix.csv file 
	 * 	<p> Example: "The winter is coming"
	 */
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getViewDate() {
		return viewDate;
	}

	public void setViewDate(String viewDate) {
		this.viewDate = viewDate;
	}

	@Override
	public String toString() {
		return "ViewItem [title=" + title + ", viewDate=" + viewDate+"]";
	}	
	
}
