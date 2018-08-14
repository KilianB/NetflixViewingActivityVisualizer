package com.github.kilianB.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.EditDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;

import com.github.kilianB.fileHandling.in.NetflixParser;
import com.github.kilianB.fileHandling.out.CSVMovieWriter;
import com.github.kilianB.fileHandling.out.CSVShowWriter;
import com.github.kilianB.fileHandling.out.CSVWriter;
import com.github.kilianB.model.BaseEntityWrapper;
import com.github.kilianB.model.netflix.NetflixMovie;
import com.github.kilianB.model.netflix.NetflixShowEpisode;
import com.github.kilianB.model.netflix.ViewItem;
import com.uwetrottmann.trakt5.entities.Episode;
import com.uwetrottmann.trakt5.entities.Movie;
import com.uwetrottmann.trakt5.entities.Season;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Type;

import trakt.TraktHelper;

/**
 * Download and append detailed information to a Netflix viewing history file e.g. runtime
 * and genre for further evaluation.
 * <p>
 * 
 * The code is in no way optimized but rather a practice project focuses on utilizing some of Java 8-10
 * features.
 * 
 * JAVA 8 Method reference :: Predicates 
 * JAVA 9 streams + lambda 
 * JAVA 10: local  * type inference
 * 
 * The input csv file will be parsed and individual items will either be classified as an 
 * episode (show), a movie or of type unknown. For each type a separate csv file with additional
 * information will be produced
 * 
 * @usage -> java -jar NetflixAnalyzer InputFilePath.csv TraktApiKey
 * 		  -> java -jar NetflixAnalyzer TraktApiKey
 * 
 * @author Kilian
 *
 */
public class NetflixAnalyzer {

	private static final Logger LOGGER = Logger.getLogger(NetflixParser.class.getName());

	// Settings

	/**
	 * Print additional debug information
	 */
	private final boolean verbose = false;

	/**
	 * Output path of csv file containing all items classified as movie
	 */
	private final String movieCsv = "MovieViewingHistory.csv";
	
	/**
	 * Output path of csv file containing all items classified as episode
	 */
	private final String showCsv = "ShowViewingHistory.csv";
	
	/**
	 * Output path of csv file containing all items which could not be resolved
	 */
	private final String unknownCsv = "UnknownViewingHistory.csv";

	
	//Fields
	
	/**
	 * Trakt client used to query the movie database
	 */
	private TraktHelper trakt;

	/**
	 * Key : ->  Series name as found in the netflix viewing history file 
	 * Value: -> Show object returned by trakt (Overview: Genre, rating, ids)
	 */
	private Map<String, Show> traktShows;

	/**
	 * Key : ->  Movie name as found in the netflix viewing history file 
	 * Value: -> Movie object returned by trakt
	 */
	private Map<NetflixMovie, Movie> traktMovies;

	/**
	 * 1. Parse the Netflix viewing csv file -> (Title,Date) 
	 * 2. Query trakt API to attach id's to the movie/series title. 
	 * 	a) Movies are done. more work for episodes are required
	 * 3. Download summary of the series to get the episode id's 
	 * 4. Use the episode ids and download granular information for each item
	 * 5. Match trakt and netflix titles using levenshtein distance
	 * 6. Output results to csv
	 * 
	 * @param viewFilePath
	 * @param traktToken
	 */
	public NetflixAnalyzer(String viewFilePath, String traktToken) {
		
		/*
		 * Initialize trakt api movie database
		 */
		trakt = new TraktHelper(traktToken);

		//@formatter:off
		try {
			
			//1.  Import viewing history and attempt to classify movies and shows 
			List<ViewItem> parsedHistory = NetflixParser.parseHistoryFile(viewFilePath);

			//Retrieve a single NetflixShow for every show watched
			HashSet<NetflixShowEpisode> distinctSeries = parsedHistory.stream()
					.filter(item -> item.isShow())
					.filter(distinctObjects(item -> ((NetflixShowEpisode) item).getSeries()))
					.map(show -> (NetflixShowEpisode) show)
					.collect(Collectors.toCollection(HashSet::new));

			//Retrieve unique movies e.g. if we have watched a movie twice we only should hit the api once
			var distinctMovies = parsedHistory.stream()
					.filter(i -> !i.isShow())
					.filter(distinctObjects(item -> ((NetflixMovie) item).getTitle()))
					.map(show -> (NetflixMovie) show)
					.collect(Collectors.toCollection(HashSet::new));
			//@formatter:on

			// Debug print
			System.out.println("Series: " + distinctSeries.size() + " " + distinctSeries.stream()
					.map(series -> series.getSeries()).collect(Collectors.joining(" , ", "Series [", "] parsed")));
			System.out.println("Movies: " + distinctMovies.size() + " " + distinctMovies.stream()
					.map(movie -> movie.getTitle()).collect(Collectors.joining(" , ", "Movies [", "] parsed")));

			if (verbose) {
				for (var item : parsedHistory) {
					if (item instanceof NetflixShowEpisode) {
						NetflixShowEpisode show = (NetflixShowEpisode) item;
						System.out.println(show);
					}
				}
			}

			/*
			 * We are not allowed to search for episode given a specified series. Only
			 * searching for the episode title will give false reports therefore download
			 * episode info for all series we have watched and search within the returned
			 * results. This approach isn't really easy on the trakt API therefore we are
			 * nice and save the returned information in an SQL database to only need to
			 * query the API once.
			 */

			//@formatter:off
			
			 //2. Extract trakt id for every series
			traktShows =  distinctSeries.parallelStream()
					.map(trakt::searchShow)
					.flatMap(Optional<BaseEntityWrapper<NetflixShowEpisode,Show>>::stream)
					.collect(Collectors.toMap(
							s -> s.netflixViewItem.getSeries(), //Key by string
							s -> s.getEntity()));	//Function.identity(); if not wrapped
			
			// 2. Extract trakt id for every movie
			traktMovies = distinctMovies.parallelStream()
					.map(trakt::searchMovie)
					.flatMap(Optional<BaseEntityWrapper<NetflixMovie,Movie>>::stream)
					.collect(Collectors.toMap(
							s -> s.netflixViewItem,
							s -> s.getEntity()));
			//@formatter:on

			if (verbose) {
				trakt.printNotFoundItems();
			}

			/**
			 * Key : -> Movie name as found in the netflix viewing history file Value: ->
			 * Movie object returned by trakt
			 */
			HashMap<Show, List<Season>> seasonList = trakt.downloadSeriesInfo(traktShows);

			Map<Type, Set<String>> notFoundOnTrakt = trakt.getNotFoundItems();

			/*
			 * Generate 3 output files 1. 2. 3.
			 */
			CSVShowWriter showWriter = new CSVShowWriter(new File(showCsv), ";");
			CSVMovieWriter movieWriter = new CSVMovieWriter(new File(movieCsv), ";");
			CSVWriter unknowWriter = new CSVWriter(new File(unknownCsv), ";", "Date", "Type", "Title", "Series");

			var notFoundShows = notFoundOnTrakt.get(Type.SHOW);
			var notFoundMovies = notFoundOnTrakt.get(Type.MOVIE);

			trakt.printNotFoundItems();
			/*
			 * Multithread calls or we will wait forever (Do we need small delays to not get
			 * blacklisted at trakt?
			 * 
			 */
			ExecutorService executor = Executors.newFixedThreadPool(15,
					(Runnable r)->{
						Thread t = new Thread(r);
						t.setName("T-Pool:");
						t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
							@Override
							public void uncaughtException(Thread t, Throwable e) {
								System.out.println("Uncaught exception: "  + t + " " + e.toString());
							}
							
						});
						return new Thread(r);
					}
			);
			
			System.out.println("Retrieve runtime for shows. This may take a few seconds...");
			
			var futures = new ArrayList<Future<?>>();
			
			/*
			 * Variables modified in runnables have to be effective final. Circumvent this issue
			 * by encapsulating the int in an object. Atomic integers also give us thread safety 
			 * for free.
			 */
			AtomicInteger knownEpisodeCount = new AtomicInteger(0);
			AtomicInteger unknownCount = new AtomicInteger(0);
			
			/*
			 * Construct the tasks of writing the individual data to a csv file. 
			 * For shows runtime data still has to be downloaded on a per episode basis 
			 */
			for (var viewItem : parsedHistory) {
				//Not the nicest use of anonymous classes but keep it for now...
				var future = executor.submit(new Runnable() {
					ViewItem viewItem;

					@Override
					public void run() {
						try {
							if (viewItem.isShow()) {

								NetflixShowEpisode netflixShow = (NetflixShowEpisode) viewItem;

								if (notFoundShows.contains(netflixShow.getSeries())) {
									LOGGER.fine("Non resolved show: " + netflixShow.getTitle());
									unknowWriter.writeLine(netflixShow.getViewDate(), Type.SHOW, netflixShow.getTitle(),
											netflixShow.getSeries());
									unknownCount.incrementAndGet();
								} else {
									// Special case. While the show object contains a runtime
									// It is not specific for an episode but more or less the average of a show.
									// Query the episode list and look again
									Show show = traktShows.get(netflixShow.getSeries());
									int runtime = getRuntimeForEpisode(netflixShow, seasonList);
									showWriter.push(show, netflixShow, runtime);
									knownEpisodeCount.incrementAndGet();
								}
								
								// Output to csv
							} else {
								// Movie
								NetflixMovie netflixMovie = (NetflixMovie) viewItem;

								if (notFoundMovies.contains(netflixMovie.getTitle())) {
									unknowWriter.writeLine(netflixMovie.getViewDate(), Type.MOVIE,
											netflixMovie.getTitle());
									unknownCount.incrementAndGet();
								} else {
									movieWriter.push(traktMovies.get(netflixMovie), netflixMovie);
								}
							}
						} catch (IOException e) {
							LOGGER.severe("Error during output file creation: " + e.getMessage());
						}
					}
					
					//Inject value into anonymous class ...
					public Runnable setViewItem(ViewItem viewItem) {
						this.viewItem = viewItem;
						return this;
					}

				}.setViewItem(viewItem));
				
				futures.add(future);
			}
			
			//Wait for all threads to return
//			for(var future : futures) {
//				try {
//					future.get();
//				} catch (InterruptedException | ExecutionException e) {
//					e.printStackTrace();
//				}
//			}
			
			try {
				executor.shutdown();
				executor.awaitTermination(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//Close writer	//TODO move in finally?
			showWriter.close();
			movieWriter.close();
			unknowWriter.close();
			
			//dumpActiveNonDeamonThreads("After Shutdown");
			
			//Print some information
	
			int charLength = (int)Math.log10(parsedHistory.size())+1;
			
			System.out.printf(
					"%nFinished:%n-----------------------------------------%n"
					+ "%14s %"+charLength+"d%n%14s %"+charLength+"d%n%14s %"+charLength+"d%n"
							+ "%14s %"+charLength+"d%n%14s %"+charLength+"d%n",
					"Items parsed:",parsedHistory.size(),"Unique shows:", traktShows.size(),
					"Episodes:", knownEpisodeCount.get(),"Movies:", traktMovies.size(),
					"Unknown:",unknownCount.get());
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Print some 
		
		
		/*The trakt2 api depends on okttp which releases it's ressources after some idle time.
		 * We don't want to wait so long and trakt doesn't expose the client therefore force
		 * all threads to shut down at this point
		 */
		System.exit(0);
		
		
	}

	/**
	 * A predicate used to retrieve unique values present in a collection based on an arbitrary 
	 * filter value
	 * 
	 * @param func function executed to retrieve the filter key from the object
	 * @return a map containing all elements which are present in the collection without duplicates
	 */
	public Predicate<ViewItem> distinctObjects(Function<? super ViewItem, Object> func) {
		var map = new HashSet<Object>();
		return t -> map.add(func.apply((ViewItem) t));
	}

	/**
	 * Return the runtime for an individual episode
	 * 
	 * @param show
	 * @param seasonList
	 * @return
	 */
	private int getRuntimeForEpisode(NetflixShowEpisode show, HashMap<Show, List<Season>> seasonList) {

		// TODO returns null!

		String episodeTitle = show.getTitle();
		String seriesName = show.getSeries();
		int seasonNumber = show.getSeason();

		// Get the Show object as retrieved by trakt
		Show traktShow = traktShows.get(seriesName);

		// Retrieve the episode number query the

		List<Season> seasons = seasonList.get(traktShow);

//		System.out.println("Netflix Show: " + show + "\nTrakt: " + traktShow + "\nSeasons: " + seasons
//				+ "\nSeasonNumber: " + seasonNumber);

		// Get the correct Season
		Optional<Season> seasonTemp = seasons.stream().filter(s -> s.number == seasonNumber).findAny();


		if (!seasonTemp.isPresent()) {
			LOGGER.warning("Could not find season of " + seriesName + "(" + seasonNumber + "). Fallback to "
					+ "defaul show runtime");
			return traktShow.runtime;
		}

		Season correctSeason = seasonTemp.get();

		// Now choose the correct episodes
		List<Episode> episodes = correctSeason.episodes;

		// Choose the episode depending on the title.
		EditDistance<Integer> levDistance = new LevenshteinDistance();
		var potentialEpisodes = new PriorityQueue<EpisodeTitleSearchResult>();
		for (var e : episodes) {
			// Calculate the similarity between the title we want and the title we get since
			// trakt and netflix titles may not be 100% identical
			int distance = levDistance.apply(episodeTitle, e.title);
			potentialEpisodes.add(new EpisodeTitleSearchResult(e, distance));
		}

		// Get the closest match

		EpisodeTitleSearchResult closestMatch = potentialEpisodes.poll();

		// Download the summary for the episode

		int editDistance = closestMatch.editDistance;
		if (editDistance < 5) {

			// Episodes downloaded via season summary don't contain the runtime.
			try {
				int runtime = trakt.getEpisodeRuntime(Integer.toString(closestMatch.episode.ids.trakt));

				if (editDistance > 0) {
					// Close enough match
					LOGGER.warning("No exact match for episode found. Going with closest match: Query:" + episodeTitle
							+ " Target: " + closestMatch.episode.title);
				}
				return runtime;

			} catch (IOException e1) {
				LOGGER.severe("Error while downloading runtime. Fallback to average show runtime");
				e1.printStackTrace();
				return traktShow.runtime;
			}
		} else {
			LOGGER.warning("Could not find episode of " + seriesName + "(" + episodeTitle + "). Fallback to "
					+ "average show runtime");
			return traktShow.runtime;
		}
	}

	/*
	 * 
	 * Helper classes
	 * 
	 */

	/**
	 * Title of episodes returned by trakt might not exactly match the titles
	 * provided by netflix. Therefore allow search through all episode titles of a
	 * season and pick the closest candidate.
	 * 
	 * @author Kilian
	 *
	 */
	class EpisodeTitleSearchResult implements Comparable<EpisodeTitleSearchResult> {
		int editDistance;
		Episode episode;

		public EpisodeTitleSearchResult(Episode e, int distance) {
			this.episode = e;
			this.editDistance = distance;
		}

		@Override
		public int compareTo(EpisodeTitleSearchResult o) {
			return Integer.compare(editDistance, o.editDistance);
		}

	}

	public static void main(String[] args) {

		//Set logging format
		System.setProperty("Djava.util.logging.SimpleFormatter.format",
				"%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");

		// Input validation
		if (args.length >= 2 && args[0].endsWith(".csv") && args[1].length() == 64) {
			// Trakt token is sha 256 encrypted? -> 64 characters
			new NetflixAnalyzer(args[0], args[1]);
		} else if(args.length == 1){
			if(args[0].length() == 64) {
				System.err.println("No input file path specified. Falback to default "
						+ "NetflixViewingHistory.csv in current directory");
				new NetflixAnalyzer("NetflixViewingHistory.csv", args[0]);
			}else {
				System.err.println("The supplied trakt key does not have the correct length to be "
						+ "a valid key");
				System.err.println("Aborting");
			}
			
		}else {
			System.err.println("Usage:\n"
					+ "\tjava -jar NetflixAnalyzer PATH_TO_VIEWHISTORYFILE.csv traktClientID\n"
					+ "\tjava -jar NetflixAnalyzer traktClientID");
			System.err.println("Aborting");
		}
	}

	
	
	/**
	 * Debug function used to check which threads prevent the jvm from exiting
	 * In production we would not use strack traces as these are rather expensive
	 * @param message
	 */
	@SuppressWarnings("unused")
	private void dumpActiveNonDeamonThreads(String message) {
		System.out.println(message);
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		for(Thread t : threadSet) {
			if(!t.isDaemon())
				System.out.println(t);
		}
	}

}
