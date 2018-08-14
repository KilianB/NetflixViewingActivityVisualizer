package com.github.kilianB.model.netflix;

/**
 *  Representing a single line found in the Netflix activity csv file
 *  most likely representing a movie. 
 * @author Kilian
 *
 */
public class NetflixMovie extends ViewItem{

	public NetflixMovie(String title, String viewDate) {
		super(title, viewDate);
	}

	@Override
	public boolean isShow() {
		return false;
	}

}
