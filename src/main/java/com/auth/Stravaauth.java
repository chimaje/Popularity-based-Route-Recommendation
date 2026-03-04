package com.auth;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Strava OAuth2 Helper
 *
 * Strava uses OAuth2. Here's the flow:
 * 1. Direct your browser to the authorization URL below
 * 2. Authorize the app — Strava redirects you to your redirect URI with a "code" param
 * 3. Exchange that code for an access token using the token endpoint
 * 4. Paste the access token into your .env file
 */
public class Stravaauth {

    private static final String AUTH_URL = "https://www.strava.com/oauth/authorize";
    private static final String TOKEN_URL = "https://www.strava.com/oauth/token";
    private static final String REDIRECT_URI = "http://localhost"; // Can be any URI for testing

    private final String clientId;
    private final String clientSecret;
    private final String accessToken;

    public Stravaauth() {
        Dotenv dotenv = Dotenv.load();
        this.clientId = dotenv.get("STRAVA_CLIENT_ID");
        this.clientSecret = dotenv.get("STRAVA_CLIENT_SECRET");
        this.accessToken = dotenv.get("STRAVA_ACCESS_TOKEN");
    }

    /**
     * Step 1: Print the URL to visit in your browser to authorize the app.
     * Scope "read" allows public segment data. "activity:read_all" unlocks starred segments.
     */
    public void printAuthorizationUrl() {
        String url = AUTH_URL
                + "?client_id=" + clientId
                + "&redirect_uri=" + REDIRECT_URI
                + "&response_type=code"
                + "&scope=read,activity:read_all";

        System.out.println("=== STEP 1: Open this URL in your browser ===");
        System.out.println(url);
        System.out.println();
        System.out.println("=== STEP 2: After authorizing, you'll be redirected to a URL like: ===");
        System.out.println("http://localhost?state=&code=YOUR_CODE_HERE&scope=read,activity:read_all");
        System.out.println();
        System.out.println("=== STEP 3: Copy the 'code' value and run this curl command: ===");
        System.out.println("curl -X POST https://www.strava.com/oauth/token \\");
        System.out.println("  -d client_id=" + clientId + " \\");
        System.out.println("  -d client_secret=" + clientSecret + " \\");
        System.out.println("  -d code=YOUR_CODE_HERE \\");
        System.out.println("  -d grant_type=authorization_code");
        System.out.println();
        System.out.println("=== STEP 4: Copy the 'access_token' from the response into your .env file ===");
    }

    public String getAccessToken() {
        if (accessToken == null || accessToken.equals("your_access_token_here")) {
            throw new IllegalStateException(
                "Access token not set! Run Main with --auth flag first, then update your .env file."
            );
        }
        return accessToken;
    }

    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
}