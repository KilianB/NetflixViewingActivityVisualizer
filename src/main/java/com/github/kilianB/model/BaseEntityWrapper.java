package com.github.kilianB.model;

import com.github.kilianB.model.netflix.ViewItem;
import com.uwetrottmann.trakt5.entities.BaseEntity;

/**
 * Bundle a ViewItem directly parsed from the viewing history 
 * file with a metadataentity downloaded from trakt
 * @author Kilian
 *
 * @param <T>	Either NetflixMovie or NetflixShowEpisode
 * @param <K>	The matching TraktDTO class
 */
public class BaseEntityWrapper<T extends ViewItem,K extends BaseEntity> {
	
	/**
	 * Netflix.csv object
	 */
	public T netflixViewItem;
	/**
	 * Trakt object
	 */
	public K entity;

	public BaseEntityWrapper(T netflixTitle, K entity) {
		this.netflixViewItem = netflixTitle;
		this.entity = entity;
	}
	
	public T getTitle() {
		return netflixViewItem;
	}

	public K getEntity() {
		return entity;
	}
}