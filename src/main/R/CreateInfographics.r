# CreateInfographics.r 
# Copyright (C) 2018  Kilian Brachtendorf 
# This code is licensed under GPL v3. For further information and the license of the images used
# please refer to http://github.com/kilianB/NetflixAnalyzer

#Helper functions

#' Install and load libaries
#'
#' @param library names in a vector to be installed and loaded. Libraries already present
#'  will not be installed. The loading occurs in the order the libs are mentioned in the vector 
#'
#' @return nothing
#' @export
#'
#' @examples
#'  installAndLoadLibraries(c("zoo","ggplot"))
installAndLoadLibraries <- function(libNames) {
  notInstalledLibs <- libNames[!(libNames %in% .packages(all = TRUE))]
  if (length(notInstalledLibs) != 0) {
    install.packages(notInstalledLibs)
  }
  invisible(lapply(libNames, FUN = require, character.only = TRUE))
}

librariesToLoad <- c("grid","extrafont","lubridate","plyr","scales","zoo",
                     "reshape2","dplyr","tidyr","ggridges","ggplot2","magick",
                     "rlist")
installAndLoadLibraries(librariesToLoad)
rm(librariesToLoad,installAndLoadLibraries)

#Load fonts before ggplot 2?

if(!"Impact" %in% fonts()){
  font_import(prompt = FALSE) 
}
loadfonts()

##Define custom date format
setClass("customDate",representation(Date = "Date"))
setAs("character","customDate",function(from) as.Date(from,format="%d/%m/%Y"))

###############################################################################################
#                                     Settings                                                #
###############################################################################################

# Path to the extracted  .csv files and images
setwd("C:/Users/Kilian/git/NetflixViewingactivityVisualizer/distribution/R");
# Path of the final infographics
outputPath <- "C:/Users/Kilian/git/NetflixViewingactivityVisualizer/distribution/R/NetflixInfographics.png"

###Theme

#Plot and image color
primaryColor    <- "#552683"
#Heatmap low range color
primaryColorBright <- "#be7cff" 
#Axis color and sections
secondaryColor  <- "#E7A922"
#Heading color
headingColor <- "white"
#Plot background color
backgroundColor <- "#E2E2E3"
#Footer text
footerColor <- "#d8d8d8"
#Summary background text
summaryColor <- "#CA8B01"
#Infographics header text
infoColor <- "#A9A8A7"


#Dark theme

# #Plot and image color
# primaryColor    <- "gray"
# #Heatmap low range color
# primaryColorBright <- "light gray" 
# #Axis color and sections
# secondaryColor  <- "light gray"
# #Heading color
# headingColor <- "white"
# #Plot background color
# backgroundColor <- "black"

netflixCostPerMonth <-  (13.99 / 4)
currency <- "€"
averageCinemaTicket <- 8.63 #https://www.statista.com/statistics/382600/cinema-ticket-price-germany/

# To calculate the cost/episode we need to know how how many months we are already paying
# 
useCurrentDate <- TRUE #FALSE


###############################################################################################
#                               Helper Function And Themes                                    #
###############################################################################################

#https://stackoverflow.com/a/42997511/3244464
#TODO fix. sunday is still last week. We shit but this causes inconsistencies 
# at months with the first day of the week being a sunday. e.g. the 1st of april 2018
firstDayOfMonth <- function(date){
  day(date) <- 1
  wday(date)
}

#' Capitalize the first letter of the given string
#'
#' @param str String 
#'
#' @return the str with the first letter capitalized
capitalize <- function(str){
  paste(toupper(substr(str, 1, 1)), substr(str, 2, nchar(str)), sep="")
}

# https://stackoverflow.com/a/26640698/3244464
elapsed_months <- function(end_date, start_date) {
  ed <- as.POSIXlt(end_date)
  sd <- as.POSIXlt(start_date)
  12 * (ed$year - sd$year) + (ed$mon - sd$mon)
}


#'String length based on viewport aestehtics consideration
#'
#' @param x string length should be evaluated of
#'
#' @return the length of the string absed on font size and font familiy of the current viewport
#' @export
#'
#' @examples
stringLength <- function(x){
  calcStringMetric(x)$width
}


#' Print a multiline multi color text on the current grid. This method attempts to mimic the 
#' same functionality as grid.print but with added support of colors
#'
#' @param x xLocation of the text in npc units
#' @param y yLocation of the text in npc units
#' @param txt vector of text being printed
#' @param col vector of colors applied to the same index of txt. Empty strings "" and online line breaks "\\n"
#'                will not be counted toward index mapping
#' @param fontSize The font size of the printed text 
#' @param fontFamiliy The font familiy of the printed text
#' @param rightAlign if TRUE right align the text. if false left align (default)
#'
#' @return nothing. prints text on plot
#' @export
#'
#' @examples 
#' grid.newpage()
#' pushViewport(viewport())
#' gridTextMulticolor(x = unit(0.5,"npc"), y = unit(0.5,"npc"), c("Red Text","Blue Text\n","Orange Text"),
#'  col = c("Red","Blue","Orange"))
gridTextMulticolor<-function(x,y,txt,col,fontSize = 1, fontFamiliy = "Impact", rightAlign = FALSE) {

  if(!is.unit(x) || !is.unit(y)){
    stop("X and Y have to be npc units")
  }
  
  #Find all line breaks which might be nested inside strings
  txt <- unlist(sapply(gsub(pattern = "\n",replacement = "$§$\n$§$",x = txt),split="$§$",fixed=TRUE,FUN = strsplit), use.names = FALSE)
  txt[txt != ""]
  
  #We need to set the cex and font families default for calcStringMetric
  #There has to be a better way e.g. using update_geom_defaults  but for now
  #just create a new viewport and use it
  currentViewPort <- current.viewport()
  pushViewport(viewport(gp = gpar(fontfamily = fontFamiliy, cex = fontSize)),recording = FALSE)
  
  thisx <- x
  thisy <- y
  
  lineHeight <- convertHeight(unit(calcStringMetric(text = "\n")$ascent,"inches"),unitTo = "npc")
  #lineHeight <- get.gpar()$lineheight 
  #Join the string to one big string and seperate it by line breaks
  
  maxLineWidth <- 
     convertWidth(
      unit(max(sapply(strsplit(paste(c(txt), collapse=""),"\n"),FUN = stringLength))
           ,units="inches"),unitTo = "npc")
  
 #To support hjust get the current line width and devide by the max line width for proper alignment
  
  colIndex <- 1
  if(!rightAlign){
    for(i in 1:length(txt)) {
      if(grepl("\n",txt[i],fixed=TRUE)){
        #Reset x
        thisx <- x
        thisy <- thisy - lineHeight
      }else{
        grid.text(vp = currentViewPort, txt[i],x = thisx, y = thisy, gp = gpar(fontfamily = fontFamiliy, col = col[colIndex], cex = fontSize),just=0)
        thisx<-thisx+convertWidth(unit(calcStringMetric(txt[i])$width,"inches"),"npc") * fontSize
        colIndex <- colIndex +1 
      }
     }
  }else{
    
    lineCount <- 1
    tokensByLine <- list()
    tokensByLine[[1]] <- list()
    for(token in txt){
      if(token == "\n"){
        lineCount <- lineCount + 1
        tokensByLine[[lineCount]] <- list()
      }else{
        tokensByLine[[lineCount]] <- append(tokensByLine[[lineCount]],token)
      }
    }
      #for each token 
    
    for(line in tokensByLine){
      
      colIndex <- (colIndex + length(line)-1)
      
      for(i in length(line):1){
        grid.text(vp = currentViewPort, line[i],x = thisx, y = thisy, gp = gpar(fontfamily = fontFamiliy, col = col[colIndex], cex = fontSize),just=1)
        thisx<-thisx-convertWidth(unit(calcStringMetric(line[i])$width,"inches"),"npc") * fontSize
        colIndex <- colIndex  - 1 
      }
      #Reset x
      thisx <- x
      thisy <- thisy - lineHeight
      
      colIndex <- colIndex + length(line)
      
    }
  }
  #Remove the artificially created viewport
  popViewport(recording = FALSE)
}

  
## function
vplayout <- function(x,y)
  viewport(layout.pos.row = x, layout.pos.col = y)

# Configure theme
kobe_theme <- function() {
  theme(
    plot.background = element_rect(fill = backgroundColor, colour = backgroundColor),
    panel.background = element_rect(fill = backgroundColor),
    axis.text = element_text(colour = secondaryColor, family = "Impact"),
    plot.title = element_text(colour = primaryColor, face = "bold", size = 18, hjust = 0.5, family = "Impact"),
    axis.title = element_text(colour = primaryColor, face = "bold", size = 13, family = "Impact"),
    panel.grid.major.x = element_line(colour = secondaryColor),
    panel.grid.minor.x = element_blank(),
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    strip.text = element_text(family = "Impact", colour = headingColor),
    strip.background = element_rect(fill = secondaryColor),
    axis.ticks = element_line(colour = secondaryColor)
  )
}

kobe_theme2 <- function() {
  theme(
    legend.position = "bottom", legend.title = element_text(family = "Impact", colour = primaryColor, size = 10),
    legend.background = element_rect(fill = backgroundColor),
    legend.key = element_rect(fill = backgroundColor, colour = backgroundColor),
    legend.text = element_text(family = "Impact", colour = secondaryColor, size = 10),
    plot.background = element_rect(fill = backgroundColor, colour = backgroundColor),
    panel.background = element_rect(fill = backgroundColor),
    axis.text = element_text(colour = secondaryColor, family = "Impact"),
    plot.title = element_text(colour = primaryColor, face = "bold", size = 18, hjust = 0.5, family = "Impact"),
    axis.title = element_text(colour = primaryColor, face = "bold", size = 13, family = "Impact"),
    panel.grid.major.y = element_line(colour = secondaryColor),
    panel.grid.minor.y = element_blank(),
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    strip.text = element_text(family = "Impact", colour = headingColor),
    strip.background = element_rect(fill = secondaryColor),
    axis.ticks = element_line(colour = secondaryColor)
  )
}



###############################################################################################
#                                    Data Prep                                                #
###############################################################################################
clockImage <- image_colorize(image_read("images/time.png"),opacity=100,color =primaryColor)
heartImage <- image_colorize(image_read("images/like.png"),opacity=100,color =primaryColor)
marathonImage <- image_colorize(image_read("images/running.png"),opacity=100,color =primaryColor)
hashtagImage <- image_colorize(image_read("images/hashtag.png"),opacity=100,color =primaryColor)
starImage <- image_colorize(image_read("images/star.png"),opacity=100,color =primaryColor)
cashImage <- image_colorize(image_read("images/cash.png"),opacity=100,color =primaryColor)
ticketImage <- image_colorize(image_read("images/ticket.png"),opacity=100,color =primaryColor)
grandpaImage <- image_colorize(image_read("images/grandpa.png"),opacity=100,color =primaryColor)
numberOne <- image_colorize(image_read("images/numberOne.png"),opacity=100,color =primaryColor)

movieHistory <- read.csv("../MovieViewingHistory.csv",sep = ";",colClasses = c("customDate","factor","integer","factor","Date","character"))
showHistory <- read.csv("../ShowViewingHistory.csv",sep =";",colClasses = c("customDate","factor","factor","integer","integer","factor","customDate","factor","character"))
unknownHistory <- read.csv("../UnknownViewingHistory.csv",sep =";",colClasses = c("customDate","factor","factor","factor"))

##Prepare genre data

movieHistory.long <- movieHistory %>% 
  mutate(Genres = strsplit(substring(movieHistory$Genres,2,nchar(movieHistory$Genres)-1),", ")) %>%
  unnest(Genres)
movieHistory.long$dummy <- 1
  #Drop unnecessary fields
genre.movie <- movieHistory.long[,6:7]

showHistory.long <- showHistory %>% 
  mutate(Genres = strsplit(substring(showHistory$Genres,2,nchar(showHistory$Genres)-1),", ")) %>%
  unnest(Genres)
showHistory.long$dummy <- 1
genre.show <- showHistory.long[,9:10]

genre <- rbind(genre.movie,genre.show)

genre <- aggregate(genre$dummy, by=list(Category=genre$Genres), FUN = sum)
colnames(genre) <- c("Genre","Count")
genre <- genre[order(genre$Count,decreasing = TRUE),]

#Some sample plots
#movieHistory.long %>%  ggplot(aes(x=Released,y=Genres)) + geom_point(size=2, shape=23) + kobe_theme()
#movieHistory %>%  ggplot(aes(x=Released,y=Runtime)) + geom_point(size=2, shape=23)
#movieHistory %>%  ggplot(aes(x=Released,y=Released)) + geom_point(size=2, shape=23)

##### Runtime

dates <- c(movieHistory$Date,showHistory$Date,unknownHistory$Date)
runtime.Total <- sum(c(movieHistory$Runtime,showHistory$Runtime))
runtime.Combined <- rbind(showHistory[,c(1,5)],movieHistory[,c(1,3)])

#Runtime by day
runtime.ByDay <- aggregate(runtime.Combined$Runtime, by=list(Category=runtime.Combined$Date), FUN = sum)
colnames(runtime.ByDay) <- c("Date","Runtime")

##Runtime by series
runtime.BySeries <- aggregate(showHistory$Runtime, by=list(Category=showHistory$Series), FUN = sum)
colnames(runtime.BySeries) <- c("Series","Runtime")

episodeCountBySeries <- showHistory
episodeCountBySeries$dummy <- 1
episodeCountBySeries <- aggregate(episodeCountBySeries$dummy, by=list(Category=episodeCountBySeries$Series), FUN = sum)
colnames(episodeCountBySeries) <- c("Series","Episode Count")

## Prepare calendar heatmap (based on http://r-statistics.co/Top50-Ggplot2-Visualizations-MasterList-R-Code.html) 

# Init heat map data
hmd <- runtime.ByDay
dayRange <- seq.Date(from = min(runtime.ByDay$Date),to= max(runtime.ByDay$Date), by ="days")


#Select all days which are not present in the original data (e.g. we didn't watch anything at those)
missingDates <- data.frame(dayRange[!(dayRange %in% runtime.ByDay$Date)])
missingDates$Runtime <- NA
colnames(missingDates)[1] <- "Date"

hmd <- rbind(runtime.ByDay,missingDates)
hmd$year <- as.numeric(format(hmd$Date,"%Y"))
hmd$month <- as.numeric(format(hmd$Date,"%m"))
hmd$monthf<-factor(hmd$month,levels=as.character(1:12),labels=c("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"),ordered=TRUE)
hmd$weekday <- as.POSIXlt(hmd$Date)$wday
##We don't work with american date formats here (sunday is 7 not 0...)
hmd$weekday[hmd$weekday==0] <- 7
hmd$weekdayf <- factor(hmd$weekday,levels=rev(1:7),labels=rev(c("Mon","Tue","Wed","Thu","Fri","Sat","Sun")),ordered=TRUE)
#Calculate the week of the moneth
hmd$yearmonth <- as.yearmon(hmd$Date)
hmd$yearmonthf<-factor(hmd$yearmonth)
hmd$weekOfMonth <- ceiling(as.numeric(day(hmd$Date) + firstDayOfMonth(hmd$Date) - 2)/7)

##Insert dummy empty values for days we have not watched anythign
heatmap <- ggplot(hmd, aes(weekOfMonth, weekdayf, fill = Runtime)) + 
  geom_tile(colour = headingColor) + facet_grid(year~monthf) + scale_fill_gradient(low=primaryColorBright, high=primaryColor, na.value = "#cecccc") +
  ggtitle("Time Watched") +  xlab("\n\nWeek of Month") + ylab("") + scale_x_continuous(limits = c(0, 5)) + 
  coord_fixed()

heatmap <- heatmap + kobe_theme2()
rm(hmd,dayRange,missingDates)





## Calculate money

if(useCurrentDate){
  monthsNetflixOwned <- elapsed_months( Sys.time(),min(dates))
}else{
  monthsNetflixOwned <- elapsed_months(max(dates),min(dates))
}

totalCost <- netflixCostPerMonth * monthsNetflixOwned

costPerHour <- round(totalCost / (runtime.Total/60),2)

cinemaVisits <- round(totalCost/averageCinemaTicket)

rm(totalCost,monthsNetflixOwned)

## Prepare data
showHistoryNetworkCount <- showHistory
showHistoryNetworkCount$dummy <- 1
showHistoryNetworkCount <- aggregate(showHistoryNetworkCount$dummy, by=list(Category=showHistoryNetworkCount$Network), FUN = sum)
showHistoryNetworkCount$grp <- "Network"

###############################################################################################
#                                     Prepare plots                                           #
###############################################################################################

  ##Movie
  movieCertificates <- movieHistory
  movieCertificates$dummy <- 1
  movieCertificates  <- aggregate(movieCertificates$dummy, by=list(Category=movieCertificates$Certificate), FUN = sum)
  movieCertificates$group <- "Rating (Movie)"
  levels(movieCertificates$Category)<- ordered(c("Unknown",levels(movieCertificates$Category)))
  movieCertificates$Category[movieCertificates$Category == "null"] <- as.factor("Unknown")
  #movieCertificates
  
  graphCountByCertificate <- ggplot(data = movieCertificates, aes(x = Category, y = x)) +
    geom_bar(stat = "identity", fill = primaryColor) + coord_polar() + xlab("Rating") + ylab("")
  graphCountByCertificate <- graphCountByCertificate + kobe_theme2()
  #graphCountByCertificate
  
  ##Show
  showCertificates <- showHistory
  showCertificates$dummy <- 1
  showCertificates  <- aggregate(showCertificates$dummy, by=list(Category=showCertificates$Certificate), FUN = sum)
  showCertificates$group <- "Rating (Movie)"
  levels(showCertificates$Category)<- ordered(c("Unknown",levels(showCertificates$Category)))
  showCertificates$Category[showCertificates$Category == "null"] <- as.factor("Unknown")
  #showCertificates
  
#Runtime
  #Calculate frequency bins hist(x, breaks=br, include.lowest=TRUE, plot=FALSE) We never use it but maybe
  # still usefull in the future?
  frequencies <- hist(movieHistory$Runtime, breaks=6, include.lowest=TRUE, plot=FALSE)
  ##TODO actually print breaks lower limit to limt before
  movieRuntimeFrequencies <- data.frame(frequencies$mids,frequencies$counts)
  colnames(movieRuntimeFrequencies) <- c("Category","x")
  movieRuntimeFrequencies$group <- "Runtime"
  
  
  ##Runtime by weekday
  runtime.ByWeekDay <-runtime.ByDay
  runtime.ByWeekDay$weekDay <- as.POSIXlt(runtime.ByWeekDay$Date)$wday
  
  runtime.ByWeekDay  <- aggregate(runtime.ByWeekDay$Runtime, by=list(Category=runtime.ByWeekDay$weekDay), FUN = sum)
  runtime.ByWeekDay$Category[runtime.ByWeekDay$Category==0] <- 7
  runtime.ByWeekDay$Category <-  factor(runtime.ByWeekDay$Category,levels=1:7,labels=c("Mon","Tue","Wed","Thu","Fri","Sat","Sun"),ordered=TRUE)
 
  graphRuntimeByDay <- ggplot(data = runtime.ByWeekDay, aes(x = Category, y = x)) +
    geom_bar(stat = "identity", fill = primaryColor) + coord_polar() + xlab("min") + ylab("")
  graphRuntimeByDay <- graphRuntimeByDay + kobe_theme2()
  
  #Runtime by month
  runtime.ByMonth <- runtime.ByDay
  runtime.ByMonth$month <- as.POSIXlt(runtime.ByMonth$Date)$mon + 1
  runtime.ByMonth  <- aggregate(runtime.ByMonth$Runtime, by=list(Category=runtime.ByMonth$month), FUN = sum)
  runtime.ByMonth$Category <-  factor(runtime.ByMonth$Category,levels=as.character(1:12),labels=c("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"),ordered=TRUE)
  graphRuntimeByMonth <- ggplot(data = runtime.ByMonth, aes(x = Category, y = x)) + xlab("min") + ylab("") +
    geom_bar(stat = "identity", fill = primaryColor) + coord_polar()  + kobe_theme2()
  
  ##Movie Genre
  barGenre <- ggplot(data = genre, aes(x = Genre, y = Count)) + geom_bar(stat = "identity", fill = primaryColor) + 
    coord_flip()   + ylab("Network") + kobe_theme2()+ ylab("Y LABEL") + xlab("X LABEL") #
  
  
#Barplot
mostWatchedSeries <- runtime.BySeries[order(runtime.BySeries$Runtime,decreasing = TRUE),][1:10,]

mostEpisodeGraph <- ggplot(data = mostWatchedSeries, aes(x = Series, y = Runtime)) + geom_bar(stat = "identity", fill = primaryColor) + 
  coord_flip()   +xlab("")+ kobe_theme() + ylab("Runtime (min)")

networkGraph <- ggplot(data = showHistoryNetworkCount, aes(x = Category, y = x)) + geom_bar(stat = "identity", fill = primaryColor) + 
  coord_flip()   +xlab("")+ kobe_theme() + ylab("Episodes")

genreGraph <- ggplot(data = genre, aes(x = Genre, y = Count)) + geom_bar(stat = "identity", fill = primaryColor) + 
  coord_flip()   +xlab("")+ kobe_theme() + ylab("Episodes & Movies") 


##Create timline chart

tenFavoriteSeries <- runtime.BySeries[order(runtime.BySeries$Runtime,decreasing = TRUE),][1:10,1]
episodesOfFavoriteSeries <- showHistory[showHistory$Series %in% tenFavoriteSeries,]


#Long format
epStartEndDateLong <- rbind(
aggregate(episodesOfFavoriteSeries$Date, by = list(episodesOfFavoriteSeries$Series), min),
aggregate(episodesOfFavoriteSeries$Date, by = list(episodesOfFavoriteSeries$Series), max)
)


#Short format
epStartEndDate <- merge(
  aggregate(episodesOfFavoriteSeries$Date, by = list(episodesOfFavoriteSeries$Series), min),
  aggregate(episodesOfFavoriteSeries$Date, by = list(episodesOfFavoriteSeries$Series), max),
  by="Group.1"
)

colnames(epStartEndDate) <- c("Series","Begin","End")
colnames(epStartEndDateLong) <- c("Series","Date")

seriesTimeline <- ggplot(epStartEndDateLong,aes(Series,Date)) + coord_flip() + kobe_theme2() + scale_y_date(date_breaks="10 days",labels=date_format("%b ‘%y"))+ 
  geom_line(size=6,color = primaryColor) + geom_point(data=episodesOfFavoriteSeries,aes(Series,Date),color=primaryColorBright, alpha = 5/10) +
  theme(axis.text.x=element_text(angle=45, hjust=1), plot.margin = margin(5.5,5.5,30,5.5)) + ggtitle("Series - Time ")

rm(tenFavoriteSeries,episodesOfFavoriteSeries,epStartEndDate)

###############################################################################################
#                                   Construct Graphics                                      #
###############################################################################################

  ySummaryTitle <- unit(0.890, "npc")
  ySummaryContent <- ySummaryTitle  - unit(0.07, "npc")
  xSummaryTitle <- unit(0.01, "npc")
  xPaddingSummaryContent <- unit(0.2, "npc")
  xPaddingSummayTitle <- unit(0.8, "npc")
  
  
  png(outputPath, width = 10, height = 25, units = "in", res = 500, type = "cairo")
  grid.newpage() 
  pushViewport(viewport(layout = grid.layout(5, 3)))
  
  #Background
  grid.rect(gp = gpar(fill = backgroundColor, col = backgroundColor))
  #Upper banner
  grid.rect(gp = gpar(fill = secondaryColor, col = secondaryColor), x = unit(0.5, "npc"), y = unit(0.85, "npc"), width = unit(1, "npc"), height = unit(0.10, "npc"))
  
  grid.text("INFOGRAPHIC", y = unit(1, "npc"), x = unit(0.5, "npc"), vjust = 1, hjust = .5, gp = gpar(fontfamily = "Impact", col = infoColor, cex = 12, alpha = 0.3))
  grid.text("Netflix Viewing Statistics", y = unit(0.94, "npc"), gp = gpar(fontfamily = "Impact", col = secondaryColor, cex = 5.8))
  
  # Date span watched
  grid.text(paste(format(min(dates), format("%d.%m.%y")),
                  "-",
                  format(max(dates), format("%d.%m.%y"))), vjust = 0, hjust = 0, x = unit(0.01, "npc"), y = unit(0.905, "npc"), gp = gpar(fontfamily = "Impact", col = primaryColor, cex = 1.2))
  
  grid.text("SUMMARY", y = unit(0.85, "npc"), x = unit(0.5, "npc"), vjust = .5, hjust = .5, gp = gpar(fontfamily = "Impact", col = summaryColor, cex = 13, alpha = 0.3))
  
  #####Section
  
  ## Show
  grid.text("SHOWS", vjust = 0, hjust = 0, x = xSummaryTitle, y = ySummaryTitle, gp = gpar(fontfamily = "Impact", col = headingColor, cex = 1.5))
  grid.raster(hashtagImage, x = unit(0.035, "npc"), y = unit(0.866, "npc"), width = unit(0.06, "npc"))
  grid.text(paste(length(unique(showHistory$Series))," Shows","\n",paste(sum(episodeCountBySeries$`Episode Count`)," Episodes")),
            vjust = 1, hjust = 0, x = unit(0.08, "npc"), y = unit(0.875, "npc"), gp = gpar(fontfamily = "Impact", col = primaryColor, cex = 1.1))
  
  grid.raster(starImage, x = unit(0.035, "npc"), y = unit(0.822, "npc"), width = unit(0.06, "npc"))
  favoriteSeriesRuntime <- runtime.BySeries[order(runtime.BySeries$Runtime,decreasing = TRUE),][1,]
  favoriteSeriesEpisode <- episodeCountBySeries[order(episodeCountBySeries$`Episode Count`,decreasing = TRUE),][1,]
  
  grid.text( paste(favoriteSeriesRuntime$Series," (",favoriteSeriesRuntime$Runtime," min)","\n",
                   favoriteSeriesEpisode$Series," (",favoriteSeriesEpisode$`Episode Count`," Episodes)"),
             vjust = 1, hjust = 0, x = unit(0.08, "npc"), y = unit(0.828, "npc"), gp = gpar(fontfamily = "Impact", col = primaryColor, cex = 1.1))
  
  rm(favoriteSeriesEpisode,favoriteSeriesRuntime)
  
  ## Records
  grid.text("MISCELLANEOUS", vjust = 0, hjust = 0.5, x = unit(0.5,"npc"), y = ySummaryTitle, gp = gpar(fontfamily = "Impact", col = headingColor, cex = 1.5))
  grid.raster(clockImage, x = unit(0.45, "npc"), y = unit(0.87, "npc"), width = unit(0.06, "npc"))
  grid.text(paste(seconds_to_period(sum(runtime.Combined$Runtime)*60)), vjust = 0, hjust = 0, x = unit(0.5, "npc"), y = unit(0.87, "npc"), gp = gpar(fontfamily = "Impact", col = primaryColor, cex = 1))
  
  grid.raster(heartImage, x = unit(0.45, "npc"), y = unit(0.84, "npc"), width = unit(0.06, "npc"))
  grid.text(capitalize(genre[1,1]), vjust = 0, hjust = 0, x = unit(0.5, "npc"), y = unit(0.84, "npc"), gp = gpar(fontfamily = "Impact", col = primaryColor, cex = 1))
  
  grid.raster(marathonImage, x = unit(0.45, "npc"), y = unit(0.815, "npc"), width = unit(0.06, "npc"))
  grid.text(paste(
    seconds_to_period(60*runtime.ByDay[order(runtime.ByDay$Runtime,decreasing = TRUE),2][1]),
    "\n",
    runtime.ByDay[order(runtime.ByDay$Runtime,decreasing = TRUE),1][1]), just = "center", x = unit(0.535, "npc"), y = unit(0.815, "npc"), gp = gpar(fontfamily = "Impact", col = primaryColor, cex = 1))
  
  
  ## Movies
  grid.text("MOVIES", vjust = 0, hjust = 0, x = unit(0.91,"npc"), y = ySummaryTitle, gp = gpar(fontfamily = "Impact", col = headingColor, cex = 1.5))
  
  grid.raster(hashtagImage, x = unit(1, "npc") - unit(0.035, "npc"), y = unit(0.866, "npc"), width = unit(0.06, "npc"))
  grid.text(paste(length(unique(movieHistory$Title))," Movies"),
            vjust = 1, hjust = 1, x = unit(0.91, "npc"), y = unit(0.87, "npc"), gp = gpar(fontfamily = "Impact", col = primaryColor, cex = 1.1))
  
  grid.raster(starImage, x = unit(1, "npc") - unit(0.035, "npc"), y = unit(0.822, "npc"), width = unit(0.06, "npc"))
  
  longestMovie <- max(movieHistory$Runtime)
  longestMovie <- movieHistory[movieHistory$Runtime == longestMovie,]
  grid.text( paste(longestMovie$Title," (",longestMovie$Runtime," min)","\n",
                   "Total ",seconds_to_period(sum(movieHistory$Runtime)*60)),
             vjust = 1, hjust = 1, x = unit(0.91, "npc"), y = unit(0.828, "npc"), gp = gpar(fontfamily = "Impact", col = primaryColor, cex = 1.1))
  
  ### Heatmap
  print(heatmap,vp = vplayout(2,1:3))
  
  
  ####### After heatmap section
  
  grid.raster(cashImage, x = unit(0.75, "npc"), y = unit(0.635,"npc"), width = unit(0.06, "npc"))

  gridTextMulticolor(unit(0.8,"npc"),
                     unit(0.635,"npc"),
                     c(costPerHour,"  ",currency,"/hour"),
                     c(secondaryColor,secondaryColor,primaryColor,primaryColor),
                     fontSize = 1.2) 
  
  grid.raster(ticketImage, x = unit(0.75, "npc"), y = unit(0.6,"npc"), width = unit(0.06, "npc"))
 
  gridTextMulticolor(unit(0.8,"npc"),
                     unit(0.6,"npc"),
                     c(cinemaVisits," x cinema visits"),
                     c(secondaryColor,primaryColor),
                     fontSize = 1.2) 
  
  #Oldest movie
  grid.raster(grandpaImage, x = unit(0.28, "npc"), y = unit(0.635,"npc"), width = unit(0.06, "npc"),
              gp = gpar(fill="black"))
  
  oldest <- movieHistory[order(movieHistory$Released, decreasing = FALSE)[1],]

  text <- c("Old: ", as.character(oldest$Title),"\n",as.character(oldest$Released))
  
  #grid.text(paste("Old: ",,), vjust = 0.5, hjust = 1, x = unit(0.23,"npc"), y = unit(0.63,"npc"), gp = gpar(fontfamily = "Impact", col = primaryColor, cex = 1.3))
  gridTextMulticolor(unit(0.23,"npc"),
                     unit(0.64,"npc"),
                     text,
                     c(secondaryColor,primaryColor,primaryColor),
                     fontSize = 1.2,rightAlign = TRUE) 
  
  rm(oldest)
  
  grid.raster(numberOne, x = unit(0.28, "npc"), y = unit(0.6,"npc"), width = unit(0.06, "npc"),
              gp = gpar(fill="black"))
  
  #Determine the first ever watched item
  
  eMovie <- min(movieHistory$Date)
  eShow <- min(showHistory$Date)
  eUnknown <- min(unknownHistory$Date)
  
  if(eMovie <= eShow && eMovie <= eUnknown){
    earliest <- movieHistory[movieHistory$Date == eMovie,][1,]$Title
  }else if(eShow <= eMovie && eShow <= eUnknown){
    temp <- showHistory[showHistory$Date == eShow,][1,]
    earliest <- paste(temp$Series,"\n",temp$Title)
    rm(temp)
  }else{
    earliest <- unknownHistory[unknownHistory$Date == eUnknown,][1,]$Title
  }
  
  #Here we have multiple values. Just pick the first one
  gridTextMulticolor(unit(0.23,"npc"),
                     unit(0.605,"npc"),
                     c("First: ",earliest),
                     c(secondaryColor,primaryColor,primaryColor),
                     fontSize = 1.2, rightAlign = TRUE) 
  
  
  
  rm(eMovie,eShow,eUnknown,earliest)
  
  ## START PLOTS
  
  grid.rect(gp = gpar(fill = secondaryColor, col = secondaryColor),x = unit(0.19,"npc"), y = unit(0.57,"npc"),width = unit(0.31,"npc"), height = unit(0.01,"npc"))
  grid.rect(gp = gpar(fill = secondaryColor, col = secondaryColor),x = unit(0.515,"npc"), y = unit(0.57,"npc"),width = unit(0.31,"npc"), height = unit(0.01,"npc"))
  grid.rect(gp = gpar(fill = secondaryColor, col = secondaryColor),x = unit(0.84,"npc"), y = unit(0.57,"npc"),width = unit(0.31,"npc"), height = unit(0.01,"npc"))
  
  grid.text("Movie Rating", vjust = 0.5, hjust = 0.5, x = unit(0.19,"npc"), y = unit(0.57,"npc"), gp = gpar(fontfamily = "Impact", col = headingColor, cex = 1))
  grid.text("Time Spent Day Of Week", vjust = 0.5, hjust = 0.5, x = unit(0.515,"npc"), y = unit(0.57,"npc"), gp = gpar(fontfamily = "Impact", col = headingColor, cex = 1))
  grid.text("Time Spent Month Of Year", vjust = 0.5, hjust = 0.5, x = unit(0.84,"npc"), y = unit(0.57,"npc"), gp = gpar(fontfamily = "Impact", col = headingColor, cex = 1))
  
  
  print(graphCountByCertificate, vp = vplayout(3, 1))
  print(graphRuntimeByDay, vp = vplayout(3, 2))
  print(graphRuntimeByMonth, vp = vplayout(3, 3))
  
  print(genreGraph, vp = vplayout(4, 1))
  print(mostEpisodeGraph, vp = vplayout(4, 2))
  print(networkGraph, vp = vplayout(4, 3))
  
  grid.rect(gp = gpar(fill = secondaryColor, col = secondaryColor),x = unit(0.19,"npc"), y = unit(0.4015,"npc"),width = unit(0.31,"npc"), height = unit(0.01,"npc"))
  grid.rect(gp = gpar(fill = secondaryColor, col = secondaryColor),x = unit(0.515,"npc"), y = unit(0.4015,"npc"),width = unit(0.31,"npc"), height = unit(0.01,"npc"))
  grid.rect(gp = gpar(fill = secondaryColor, col = secondaryColor),x = unit(0.84,"npc"), y = unit(0.4015,"npc"),width = unit(0.31,"npc"), height = unit(0.01,"npc"))
  
  grid.text("Genres", vjust = 0.5, hjust = 0.5, x = unit(0.19,"npc"), y = unit(0.4015,"npc"), gp = gpar(fontfamily = "Impact", col = headingColor, cex = 1))
  grid.text("Most Watched Series", vjust = 0.5, hjust = 0.5, x = unit(0.515,"npc"), y = unit(0.4015,"npc"), gp = gpar(fontfamily = "Impact", col = headingColor, cex = 1))
  grid.text("Network", vjust = 0.5, hjust = 0.5, x = unit(0.84,"npc"), y = unit(0.4015,"npc"), gp = gpar(fontfamily = "Impact", col = headingColor, cex = 1))
  
  
  print(seriesTimeline,vp = vplayout(5,1:3))
  
  
  footerText <- paste("Generate your own graphics: http://www.github.com/kilianB/         - Image licenses available @github","\n","Restrictions due to data provided by Netflix:","Runtime overestimation: every episode/movie started will be counted as fully seen","\n",
                      "Runtime underestimation: Duplicate episodes/movies are not counted twice. The oldest entry will be purged from the data", " Viewing activity only contains the last 12 months of data.")
  
  grid.rect(x = unit(0.5,"npc"),y=unit(0,"npc"),width = unit(1,"npc"),height = unit(0.05,"npc"),gp = gpar(fill = secondaryColor, col = secondaryColor))
  grid.text(footerText, x = unit(0.01,"npc"), y = unit(0.015,"npc"),gp = gpar(col = footerColor, cex = 0.7), just = "left")
 
  dev.off()

  