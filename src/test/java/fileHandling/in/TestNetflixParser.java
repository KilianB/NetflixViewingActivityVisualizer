package fileHandling.in;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;

import com.github.kilianB.fileHandling.in.NetflixParser;
import com.github.kilianB.model.netflix.NetflixMovie;
import com.github.kilianB.model.netflix.NetflixShowEpisode;
import com.github.kilianB.model.netflix.ViewItem;

/**
 * 
 * @author Kilian
 *	TODO we don't have fail test cases.
 */
class TestNetflixParser {

	static Method parseMethod;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {

		// Cache reflection method
		Optional<Method> parseCandidate = ReflectionSupport.findMethod(NetflixParser.class, "parseEntry", String.class,String.class);

		if (parseCandidate.isPresent()) {
			parseMethod = parseCandidate.get();
		} else {
			throw new NoSuchMethodException("Could not find parseEntry method in CSVHelper.class");
		}

	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@Nested
	@DisplayName("Test Parsing Methods")
	class TestCSVParser {

		@Nested
		class TestSeries {
			
			@Test
			void basicSeries() {
				String title = "Title";
				int season = 1;
				String series = "TestSeries";
				
				NetflixShowEpisode show = new NetflixShowEpisode(title, "1.1.2018",season,series);
				validateEntry(show, title, season,series);
			}
			
			@Test
			void defaultSeriesName() {
				String inputString = "Lie to Me: Season 3: In the Red";
				ViewItem parsedShow = (ViewItem) ReflectionSupport.invokeMethod(parseMethod, null,inputString,"1.1.2018");
				validateEntry(parsedShow, "In the Red", 3,"Lie to Me");
			}
			
			@Test
			void numericalSeriesName() {
				String inputString = "Touch: Season 1: 1 + 1 = 3";
				ViewItem parsedShow = (ViewItem) ReflectionSupport.invokeMethod(parseMethod, null,inputString,"1.1.2018");
				validateEntry(parsedShow, "1 + 1 = 3", 1,"Touch");
			}
			
			@Test
			void colonsInSeriesName() {
				String inputString = "Star Trek: Discovery: Season 1: Context is for Kings";
				ViewItem parsedShow = (ViewItem) ReflectionSupport.invokeMethod(parseMethod, null,inputString,"1.1.2018");
				validateEntry(parsedShow, "Context is for Kings", 1,"Star Trek: Discovery");
			}
			
			@Test 
			void colonsInEpisodeName() {
				String inputString = "The Fresh Prince of Bel-Air: Season 1: Someday Your Prince Will Be in Effect: Part 2";
				ViewItem parsedShow = (ViewItem) ReflectionSupport.invokeMethod(parseMethod, null,inputString,"1.1.2018");
				validateEntry(parsedShow, "Someday Your Prince Will Be in Effect: Part 2", 1,"The Fresh Prince of Bel-Air");
			}
			
			
			void validateEntry(ViewItem data, String title,int season,String series) {
				TestCSVParser.this.validateEntry(NetflixShowEpisode.class, data, title, season,series);
			}
			
		}

		@Nested
		class TestMovies {

			@Test
			void movieWithoutColon() {
				String movieTitle = "The Legend of Tarzan";
				ViewItem parsedMovie = (ViewItem) ReflectionSupport.invokeMethod(parseMethod, null,
						movieTitle,"ViewDate");
				validateEntry(parsedMovie, movieTitle);
			}

			@Test
			void movieWithColon() {
				String movieTitle = "Underworld: Awakening";
				ViewItem parsedMovie = (ViewItem) ReflectionSupport.invokeMethod(parseMethod, null,
						movieTitle,"ViewDate");
				validateEntry(parsedMovie, movieTitle);
			}
			
			//As expected this will assert to be a show. 
			@Test
			@Disabled
			void movieWithTwoColons() {
				String movieTitle = "\"Underworld: Awakening : Hello\"";
				ViewItem parsedMovie = (ViewItem) ReflectionSupport.invokeMethod(parseMethod, null,
						wrapMovieTitle(movieTitle));
				validateEntry(parsedMovie, movieTitle);
			}
			

			void validateEntry(ViewItem data, String title) {
				TestCSVParser.this.validateEntry(NetflixMovie.class, data, title, Integer.MIN_VALUE,null);
			}
		}

		/**
		 * Helper method to assert if the supplied view item contains the information expected
		 * @param clazz
		 * @param data
		 * @param title
		 * @param episode
		 */
		void validateEntry(Class clazz, ViewItem data, String title, int season, String series) {
			
			assertEquals(clazz,data.getClass());
			
			//Assert movie
			if(data.getClass().equals(NetflixMovie.class)) {
				
				//@formatter:off
				assertAll(
						() -> assertFalse(data.isShow()),
						() -> assertEquals(title,data.getTitle()),
						() -> assertEquals("ViewDate",data.getViewDate())
						);
				//@formatter:on
				
			}else {
				
				NetflixShowEpisode show = (NetflixShowEpisode) data;
				assertAll(
						() -> assertTrue(show.isShow()),
						() -> assertEquals(title,show.getTitle()),
						() -> assertEquals("1.1.2018",show.getViewDate()),
						() -> assertEquals(season,show.getSeason()),
						() -> assertEquals(series,show.getSeries())
						);
			}
			
					
		}

		/**
		 * Wrap the movie title into an object the reflection method can work with
		 * 
		 * @param movieTitle
		 */
		Object[] wrapMovieTitle(String movieTitle) {
			return new Object[] { new String[] { movieTitle, "ViewDate" } };
		}
	}


}
