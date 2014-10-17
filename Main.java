package com.company;

/**
 *	Jaak Erisalu 135192IAPB
 *  Twitter Scan
 *  17.10.2014
 */

import org.apache.commons.io.IOUtils;
import twitter4j.*;
import twitter4j.auth.OAuth2Token;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    static String TWITTER_CUSTOMER_KEY = "oJe9aJXe6x1EXNp9XSyPh9bTD";
    static String TWITTER_CUSTOMER_SECRET = "pDCzy5R9EQcKHRgj8EQQYDjeJGQcJw6H5QQKP4GXkYe25obRzC";

    // Unused personal tokens for good measure
    String TWITTER_ACCESS_TOKEN = "2834773023-lq2sn2G5xFlXGxbdCFgjv5nXx6HRY8Nzk8QI2xl";
    String TWITTER_ACCESS_TOKEN_SECRET = "4hk0H8N708XH3iBwOMUrTbdxz4uaAy8UcKzrOCYV3oVUW";


    public static void main(String[] args) throws IOException {
        String location = "Tallinn";
        int amount = 20;
        String search = "";

        System.out.println("Trying to find location: " + location);
        if (search.length() > 0) {
            System.out.println("Trying to find " + amount + " tweets using the word " + search);
        }
        else {
            System.out.println("Trying to find " + amount + " tweets");
        }
        System.out.println("--------------------------------------------------------------------------------");

        getTweets(getCoordinates(location), amount, search);
    }

    /**
     * A function to get the geographic coordinates and an approximate radius of
     * a geographic location based on it's name.
     *
     * @param locationName - The place who's tweets you are interested in
     * @return - A list of doubles - [latitude, longitude, radius]
     * @throws IOException
     */
    public static List<Double> getCoordinates(String locationName) throws IOException {
        String url = "http://nominatim.openstreetmap.org/search?";
        String charset = "UTF-8";
        String format = URLEncoder.encode("xml", charset);
        String location = URLEncoder.encode(locationName, charset);
        String limit = URLEncoder.encode("1", charset);

        // Combine the parameters
        String query = String.format("q=%s&format=%s&limit=%s", location, format, limit);

        // Set up the connection
        URLConnection connection = new URL(url + query).openConnection();
        connection.setRequestProperty("Accept-Charset", charset);

        // Response to string
        InputStream response = connection.getInputStream();
        StringWriter writer = new StringWriter();
        IOUtils.copy(response, writer, charset);
        String data = writer.toString();

        // Regex out the parameters we need
        Pattern r = Pattern.compile("(lat='-?\\d+.\\d+')|(lon='-?\\d+.\\d+')|(boundingbox=\"(\\d+\\S+)\")");
        Matcher m = r.matcher(data);

        String box;
        double lat = 0, lon = 0;
        List<String> boxcoords = null;
        while (m.find()) {
            String temp = m.group(0);
            if (temp.startsWith("boundingbox")) {
                box = temp.replaceAll("[^.,\\-\\d]", "");
                boxcoords = Arrays.asList(box.split(","));
            }
            else if (m.group(0).startsWith("lat")) {
                lat = Double.parseDouble(temp.replaceAll("[^.\\-,\\d]", ""));
            }
            else {
                lon = Double.parseDouble(temp.replaceAll("[^.\\-,\\d]", ""));
            }
        }

        // Find the radius using a Great Circle algorithm
        double x1 = Math.toRadians(lat);
        double x2 = Math.toRadians(Double.parseDouble(boxcoords.get(0)));
        double y1 = Math.toRadians(lon);
        double y2 = Math.toRadians(Double.parseDouble(boxcoords.get(2)));

        // Great circle distance in radians
        double angle1 = Math.acos(Math.sin(x1) * Math.sin(x2)
                + Math.cos(x1) * Math.cos(x2) * Math.cos(y1 - y2));

        // Convert back to degrees
        angle1 = Math.toDegrees(angle1);

        // Each degree on a great circle of Earth is 60 nautical miles, 1 nautical mile is 1.852 km
        double radius = 60 * angle1 * 1.852;

        return Arrays.asList(lat, lon, radius);
    }

    /**
     * A function to get tweets posted in a geographic location
     *
     * @param coordinates - A list of [latitude, longitude, radius (in km)]
     * @param amount - The amount of tweets to query
     */
    public static void getTweets(List<Double> coordinates, int amount, String query) {
        // Auth handling code copied from project description

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setApplicationOnlyAuthEnabled(true);
        cb.setOAuthConsumerKey(TWITTER_CUSTOMER_KEY)
                .setOAuthConsumerSecret(TWITTER_CUSTOMER_SECRET);

        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter4j.Twitter twitter = tf.getInstance();

        OAuth2Token token;
        try {
            token = twitter.getOAuth2Token();
        } catch (TwitterException e1) {
            e1.printStackTrace();
        }

        double lat = coordinates.get(0);
        double lon = coordinates.get(1);
        double radius = coordinates.get(2);

        QueryResult result = null;
        try {
            result = twitter.search(new Query().geoCode(new GeoLocation(lat,lon), radius, "km").count(amount).query(query));
        } catch (TwitterException e) {
            e.printStackTrace();
        }

        for (Status status : result.getTweets()) {
            System.out.println("@" + status.getUser().getScreenName() + ": " + status.getText());
        }
    }
}
