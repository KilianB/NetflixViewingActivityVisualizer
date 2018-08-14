package trakt;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.github.kilianB.model.BaseEntityWrapper;
import com.github.kilianB.model.netflix.NetflixMovie;
import com.github.kilianB.model.netflix.NetflixShowEpisode;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.BaseEntity;
import com.uwetrottmann.trakt5.entities.Movie;
import com.uwetrottmann.trakt5.entities.SearchResult;
import com.uwetrottmann.trakt5.entities.Season;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.IdType;
import com.uwetrottmann.trakt5.enums.Type;
import com.uwetrottmann.trakt5.services.Episodes;
import com.uwetrottmann.trakt5.services.Search;
import com.uwetrottmann.trakt5.services.Seasons;

import retrofit2.Response;

public class TraktHelper {

	private static final Logger LOGGER = Logger.getLogger(TraktHelper.class.getName());

	private final String clientId;

	/**
	 * 
	 * Trakt API to access movie and show metadata
	 * 
	 * @see https://trakt.tv/
	 */
	private TraktV2 trakt;

	/**
	 * Keep track of series and movies we could not find by querying the trakt
	 * database
	 */
	private Map<Type, Set<String>> notFoundOnTrakt = new HashMap<>();

	// Cache end points
	private Search searchEndpoint;
	private Seasons seasonEndpoint;
	private Episodes episodeEndpoint;

	public TraktHelper(String clientId) {
		this.clientId = clientId;
		setup();
	}

	private void setup() {
		trakt = new TraktV2(clientId);
		searchEndpoint = trakt.search();
		seasonEndpoint = trakt.seasons();
		episodeEndpoint = trakt.episodes();
		
		// Setup hashsets
		notFoundOnTrakt.put(Type.SHOW, ConcurrentHashMap.newKeySet());
		notFoundOnTrakt.put(Type.MOVIE, ConcurrentHashMap.newKeySet());
		
		
	}

	@SuppressWarnings("unchecked")
	/**
	 * Wraps the optional show with the original showName used to search the title
	 * as the title delivered by netflix might not be identical with the title used
	 * by trakt. This might be unnecessary but prevents annoying cornver cases.
	 * 
	 * @param showName
	 * @return
	 */
	public Optional<BaseEntityWrapper<NetflixShowEpisode, Show>> searchShow(NetflixShowEpisode netflixShow) {
		Optional<Show> show = (Optional<Show>) queryTraktAPI(netflixShow.getSeries(), Type.SHOW);
		if (show.isPresent()) {
			
			//System.out.println("Show acquired: Size Genres" + show.get().genres.size() + " Certificate:" + show.get().certification.length());
			
			return Optional.of(new BaseEntityWrapper<NetflixShowEpisode, Show>(netflixShow, show.get()));
		} else {
			return Optional.empty();
		}
	}

	@SuppressWarnings("unchecked")
	public Optional<BaseEntityWrapper<NetflixMovie, Movie>> searchMovie(NetflixMovie netflixMovie) {
		Optional<Movie> movie = (Optional<Movie>) queryTraktAPI(netflixMovie.getTitle(), Type.MOVIE);
		if (movie.isPresent()) {
			return Optional.of(new BaseEntityWrapper<NetflixMovie, Movie>(netflixMovie, movie.get()));
		} else {
			return Optional.empty();
		}
	}

	/*
	 * Duplicate code following. Maybe use a functional interface or simply a
	 * util.Function and pass it along ? .apply
	 */

	private Optional<? extends BaseEntity> queryTraktAPI(String title, Type showOrMovie) {

		// TODO remove on "production"
		assert (showOrMovie.equals(Type.SHOW) || showOrMovie.equals(Type.MOVIE));

		try {
			LOGGER.config("Search for: " + title);

			Response<List<SearchResult>> results = searchEndpoint
					.textQuery(showOrMovie, title, null, null, null, null, null, null, Extended.FULL, 1, 1).execute();
			if (results.isSuccessful()) {

				if (results.body().size() == 0) {
					notFoundOnTrakt.get(showOrMovie).add(title);
					LOGGER.warning(
							"Could not find : " + (showOrMovie.equals(Type.SHOW) ? "series " : "movie ") + title);
					return Optional.empty();
				} else {

					var result = results.body().get(0);

					if (result.score < 1000) {
						LOGGER.warning(title + " matched with a low similarity score of " + result.score + " :"
								+ (showOrMovie.equals(Type.SHOW) ? result.show.title : result.movie.title));

					}

					if (showOrMovie.equals(Type.SHOW)) {
						return Optional.of(result.show);
					} else if (showOrMovie.equals(Type.MOVIE)) {
						return Optional.of(result.movie);
					} else {
						LOGGER.severe(
								"queryTraktAPI does not handle types of type:" + showOrMovie + " Aborted: " + title);
						return Optional.empty();
					}
				}
			} else {
				LOGGER.severe("Search request for - " + showOrMovie + " was not successfull. Please try again later");
				return Optional.empty();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	/**
	 * Download detailed information about every season for a series
	 * 
	 * @return
	 * @throws IOException
	 */
	public HashMap<Show, List<Season>> downloadSeriesInfo(Map<String, Show> traktShows) throws IOException{
		var seasonList = new HashMap<Show, List<Season>>();

		for (Entry<String, Show> entry : traktShows.entrySet()) {
			Show show = entry.getValue();
			var response = seasonEndpoint.summary(show.ids.slug, Extended.EPISODES).execute();

			if (response.isSuccessful()) {
				List<Season> seasons = response.body();
				seasonList.put(show, seasons);
			}else {
				System.out.println("Download Season info not sucessfull");
			}
		}

		return seasonList;
	}
	
	
	public int getEpisodeRuntime(String traktId) throws IOException {
		
		Response<List<SearchResult>> result = searchEndpoint.idLookup(IdType.TRAKT, traktId, Type.EPISODE, Extended.FULL, 1, 1).execute();
		if(result.isSuccessful()) {
			SearchResult res = result.body().get(0);
			return res.episode.runtime;
		}else {
			return -1;
		}
	}
	
	
	public Map<Type, Set<String>> getNotFoundItems() {
		return notFoundOnTrakt;
	}
	
	public void printNotFoundItems() {
		System.out.println("Not found shows: " + Arrays.toString(notFoundOnTrakt.get(Type.SHOW).toArray()));
		System.out.println("Not found movies: " + Arrays.toString(notFoundOnTrakt.get(Type.MOVIE).toArray()));
	}

}
