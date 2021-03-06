package com.r2src.dyad.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.r2src.dyad.Account;
import com.r2src.dyad.ServerException;

/**
 * A request to register an account with the Dyad Server. Also valid for already
 * registered accounts. In that case it updates the session token.
 */
public class RegisterRequest extends Request {

	private static final String PATH = "/v1/register";

	/**
	 * Creates a new registration request.
	 * 
	 * @param authToken
	 *            An OAuth2 token with the following scope:
	 *            "oauth2:https://www.googleapis.com/auth/userinfo.profile"
	 * 
	 * @param c2dm_id
	 *            The C2DM registration id of the device.
	 */
	public RegisterRequest(String authToken) {
		request = new HttpPost(PATH);
		JSONObject body = new JSONObject();
		HttpEntity entity;
		try {
			body.put("token", authToken);
			entity = new StringEntity(body.toString());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		((HttpPost) request).setEntity(entity);
	}

	/**
	 * Nothing, really.
	 * 
	 * TODO: Invent some kind of middleware to filter out and store the session
	 * token
	 */
	@Override
	public void onFinished(HttpResponse response, Account account)
			throws ServerException, IOException {

		JSONObject body;
		switch (response.getStatusLine().getStatusCode()) {

		// logged into existing account
		case 200:
			break;
		
		// registered a new account TODO: implement this in Dyad Server
		case 201:
			break;

		case 401:
			// throw new AuthTokenExpiredException();
			// TODO: The auth token has expired.
			// Invalidate Token, Request a new one,
			// compare it to the old one. If different,
			// send the new one to the server. If the same,
			// throw an error

			// any kind of error
		default:
			throw new ServerException(response);
		}

		HttpEntity entity = response.getEntity();
		try {
			body = new JSONObject(EntityUtils.toString(entity));
			account.setSessionToken(body.getString("session_token"));
		} catch (ParseException e) {
			throw new ServerException(e, response);
		} catch (JSONException e) {
			throw new ServerException(e, response);
		}
	}
}
