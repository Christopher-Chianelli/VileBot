package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.Regex;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@ApplicationScoped
public class WeatherService
{
    private static final Logger logger = Logger.getLogger( WeatherService.class );

    final String DEFAULT_LOCATION = "ytz";

    Map<String, URL> weatherFeedsByIataCode;

    Map<String, WeatherData> weatherDataByIataCode;

    final long weatherDataCacheTime = 1000 * 60 * 30;

    @PostConstruct
    public void setup()
        throws MalformedURLException
    {
        weatherDataByIataCode = new HashMap<>();
        weatherFeedsByIataCode = new HashMap<>();
        weatherFeedsByIataCode.put( "yxu", new URL( "https://weather.gc.ca/rss/city/on-137_e.xml" ) );
        weatherFeedsByIataCode.put( "yyz", new URL( "https://weather.gc.ca/rss/city/on-143_e.xml" ) );
        weatherFeedsByIataCode.put( "ygk", new URL( "https://weather.gc.ca/rss/city/on-69_e.xml" ) );
        weatherFeedsByIataCode.put( "yeg", new URL( "https://weather.gc.ca/rss/city/ab-50_e.xml" ) );
        weatherFeedsByIataCode.put( "yyc", new URL( "https://weather.gc.ca/rss/city/ab-52_e.xml" ) );
        weatherFeedsByIataCode.put( "yul", new URL( "https://weather.gc.ca/rss/city/qc-147_e.xml" ) );
        weatherFeedsByIataCode.put( "yvr", new URL( "https://weather.gc.ca/rss/city/bc-74_e.xml" ) );
        weatherFeedsByIataCode.put( "yhm", new URL( "https://weather.gc.ca/rss/city/on-77_e.xml" ) );
        weatherFeedsByIataCode.put( "ytz", new URL( "https://weather.gc.ca/rss/city/on-128_e.xml" ) );
        weatherFeedsByIataCode.put( "ykz", new URL( "https://weather.gc.ca/rss/city/on-85_e.xml" ) );
        weatherFeedsByIataCode.put( "ykf", new URL( "https://weather.gc.ca/rss/city/on-82_e.xml" ) );
        weatherFeedsByIataCode.put( "yhz", new URL( "https://weather.gc.ca/rss/city/ns-19_e.xml" ) );
        weatherFeedsByIataCode.put( "yfc", new URL( "https://weather.gc.ca/rss/city/nb-29_e.xml" ) );
        weatherFeedsByIataCode.put( "yyt", new URL( "https://weather.gc.ca/rss/city/nl-24_e.xml" ) );
        weatherFeedsByIataCode.put( "yyg", new URL( "https://weather.gc.ca/rss/city/pe-5_e.xml" ) );
        weatherFeedsByIataCode.put( "yqt", new URL( "https://weather.gc.ca/rss/city/on-100_e.xml" ) );
        weatherFeedsByIataCode.put( "ywg", new URL( "https://weather.gc.ca/rss/city/mb-38_e.xml" ) );
        weatherFeedsByIataCode.put( "yqr", new URL( "https://weather.gc.ca/rss/city/sk-32_e.xml" ) );
        weatherFeedsByIataCode.put( "yxe", new URL( "https://weather.gc.ca/rss/city/sk-40_e.xml" ) );
        weatherFeedsByIataCode.put( "ymm", new URL( "https://weather.gc.ca/rss/city/ab-20_e.xml" ) );
        weatherFeedsByIataCode.put( "yxs", new URL( "https://weather.gc.ca/rss/city/bc-79_e.xml" ) );
        weatherFeedsByIataCode.put( "yyj", new URL( "https://weather.gc.ca/rss/city/bc-85_e.xml" ) );
        weatherFeedsByIataCode.put( "ylw", new URL( "https://weather.gc.ca/rss/city/bc-48_e.xml" ) );
        weatherFeedsByIataCode.put( "yka", new URL( "https://weather.gc.ca/rss/city/bc-45_e.xml" ) );
        weatherFeedsByIataCode.put( "yow", new URL( "https://weather.gc.ca/rss/city/on-118_e.xml" ) );
        weatherFeedsByIataCode.put( "yxy", new URL( "https://weather.gc.ca/rss/city/yt-16_e.xml" ) );
        weatherFeedsByIataCode.put( "yzf", new URL( "https://weather.gc.ca/rss/city/nt-24_e.xml" ) );
        weatherFeedsByIataCode.put( "yfb", new URL( "https://weather.gc.ca/rss/city/nu-21_e.xml" ) );
        weatherFeedsByIataCode.put( "ylt", new URL( "https://weather.gc.ca/rss/city/nu-22_e.xml" ) );
    }

    @OnChannelMessage( "!forecast" )
    public String forecastWeather()
    {
        return forecastWeather( DEFAULT_LOCATION );
    }

    @OnChannelMessage( "!forecast @areaCode" )
    public String forecastWeather( String areaCode )
    {
        String locationCode = areaCode.toLowerCase();
        if ( weatherFeedsByIataCode.containsKey( locationCode ) )
        {
            WeatherData weather = getWeatherFor( locationCode );
            if ( weather == null )
            {
                return "Error reading weather feed for " + locationCode;
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                for ( Map.Entry<String, String> forecastDay : weather.getForecast().entrySet() )
                {
                    sb.append( "[" );
                    sb.append( forecastDay.getKey() );
                    sb.append( ": " );
                    sb.append( forecastDay.getValue() );
                    sb.append( "] " );
                }
                return sb.toString();
            }
        }
        else
        {
            return "No area code " + areaCode + "exists";
        }
    }

    @OnChannelMessage( "!lessweather" )
    public String onLessWeather()
    {
        return currentWeather( DEFAULT_LOCATION, "less" );
    }

    @OnChannelMessage( "!lessweather @areaCode" )
    public String onLessWeather( String areaCode )
    {
        return currentWeather( areaCode, "less" );
    }

    @OnChannelMessage( "!moreweather" )
    public String onMoreWeather()
    {
        return currentWeather( DEFAULT_LOCATION, "more" );
    }

    @OnChannelMessage( "!moreweather @areaCode" )
    public String onMoreWeather( String areaCode )
    {
        return currentWeather( areaCode, "more" );
    }

    @OnChannelMessage( "!weather" )
    public String onWeather()
    {
        return currentWeather( DEFAULT_LOCATION, "" );
    }

    @OnChannelMessage( "!weather @areaCode" )
    public String onWeather( String areaCode )
    {
        return currentWeather( areaCode, "" );
    }

    private String currentWeather( String areaCode, String modifier )
    {
        String locationCode = areaCode;
        locationCode = locationCode.toLowerCase();

        if ( weatherFeedsByIataCode.containsKey( locationCode ) )
        {
            WeatherData weather = getWeatherFor( locationCode );
            if ( weather == null )
            {
                return "Error reading weather feed for " + locationCode;
            }
            else
            {
                StringBuilder out = new StringBuilder();
                if ( !"less".equals( modifier ) && !weather.getAlerts().isEmpty() )
                {
                    out.append( String.join( "\n", weather.getAlerts() ) ).append( '\n' );
                }

                LinkedHashMap<String, String> currentConditions = weather.getCurrentConditions();
                if ( "".equals( modifier ) )
                {

                    String sb = currentConditions.get( "Condition" ) + ", Temperature: "
                        + currentConditions.get( "Temperature" ) + ", Humidity: " + currentConditions.get( "Humidity" )
                        + " - " + currentConditions.get( "Observed at" );
                    out.append( sb );
                }
                else if ( "less".equals( modifier ) )
                {
                    // bot.sendPrivmsgAs( LESS_NICK, event.getChannel(),
                    // "IT'S " + currentConditions.get( "Condition" ).toUpperCase() );
                    out.append( "IT'S " + currentConditions.get( "Condition" ).toUpperCase() );
                }
                else if ( "more".equals( modifier ) )
                {
                    StringBuilder sb = new StringBuilder();
                    for ( Map.Entry<String, String> condition : currentConditions.entrySet() )
                    {
                        sb.append( "[" );
                        sb.append( condition.getKey() );
                        sb.append( ": " );
                        sb.append( condition.getValue() );
                        sb.append( "] " );
                    }
                    out.append( sb );
                }
                return out.toString();
            }
        }
        else
        {
            return "No weather feed available for " + locationCode;
        }
    }

    private WeatherData getWeatherFor( String locationCode )
    {
        if ( weatherDataByIataCode.containsKey( locationCode ) )
        {
            WeatherData weather = weatherDataByIataCode.get( locationCode );

            long timeDiff = new Date().getTime() - weather.getCreationDate().getTime();
            // Cache for at most half an hour
            if ( timeDiff < weatherDataCacheTime )
            {
                return weather;
            }
        }

        URL feedSource = weatherFeedsByIataCode.get( locationCode );
        WeatherData weather = null;
        try
        {
            weather = new WeatherData( feedSource );
            weatherDataByIataCode.put( locationCode, weather );
        }
        catch ( IllegalArgumentException | IOException | FeedException e )
        {
            logger.error( e.getMessage() );
            logger.error( "Error opening RSS feed" );
        }
        return weather;
    }

    /**
     * Stores weather data from http://weather.gc.ca/rss/city/* rss feeds flexibly.
     */
    private static class WeatherData
    {
        private final Pattern alertIDPattern = Pattern.compile( ".*_w[0-9]+:[0-9]{14}$" );

        private final Pattern currentConditionsIDPattern = Pattern.compile( ".*_cc:[0-9]{14}$" );

        private final Pattern forecastIDPattern = Pattern.compile( ".*_fc[0-9]:[0-9]{14}$" );

        private final LinkedList<String> alerts;

        private final LinkedHashMap<String, String> currentConditions;

        private final LinkedHashMap<String, String> forecast;

        private final Date creationTime;

        WeatherData( URL feedSource )
            throws IOException, IllegalArgumentException, FeedException
        {
            alerts = new LinkedList<>();
            forecast = new LinkedHashMap<>();
            currentConditions = new LinkedHashMap<>();
            creationTime = new Date();

            try ( XmlReader reader = new XmlReader( feedSource ) )
            {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build( reader );

                for ( Object rawEntry : feed.getEntries() )
                {
                    if ( rawEntry instanceof SyndEntry )
                    {
                        SyndEntry entry = (SyndEntry) rawEntry;

                        if ( alertIDPattern.matcher( entry.getUri() ).matches() )
                        {
                            String desc = entry.getDescription().getValue();
                            if ( !desc.equals( "No watches or warnings in effect." ) )
                            {
                                alerts.add( desc );
                            }
                        }
                        else if ( currentConditionsIDPattern.matcher( entry.getUri() ).matches() )
                        {
                            String desc = entry.getDescription().getValue();
                            desc = desc.replaceAll( "<[a-zA-Z/]+>", "" );
                            desc = desc.replace( "&#176;", "°" );
                            for ( String line : desc.split( "\n" ) )
                            {
                                int sepPos = line.indexOf( ':' );
                                currentConditions.put( line.substring( 0, sepPos ),
                                                       line.substring( sepPos + 1 ).trim() );
                            }
                        }
                        else if ( forecastIDPattern.matcher( entry.getUri() ).matches() )
                        {
                            String title = entry.getTitle();
                            int sepPos = title.indexOf( ':' );
                            forecast.put( title.substring( 0, sepPos ), title.substring( sepPos + 1 ).trim() );
                        }
                    }
                }
            }
        }

        LinkedList<String> getAlerts()
        {
            return alerts;
        }

        LinkedHashMap<String, String> getCurrentConditions()
        {
            return currentConditions;
        }

        LinkedHashMap<String, String> getForecast()
        {
            return forecast;
        }

        Date getCreationDate()
        {
            return creationTime;
        }

        @Override
        public String toString()
        {
            return "WeatherData [alerts=" + alerts + ", currentConditions=" + currentConditions + ", forecast="
                + forecast + ", creationTime=" + creationTime + "]";
        }
    }
}