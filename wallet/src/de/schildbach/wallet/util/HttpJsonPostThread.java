/*
 * Copyright 2013-2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.res.AssetManager;
import de.schildbach.wallet.Constants;

/**
 * @author Andreas Schildbach
 */
public abstract class HttpJsonPostThread extends HttpThread
{
	private static final Logger log = LoggerFactory.getLogger(HttpJsonPostThread.class);
	private JSONObject body;

	public HttpJsonPostThread(@Nonnull final JSONObject body,
			@Nonnull final String url,
			@Nullable final String userAgent)
	{
		super(null, url, userAgent);
		this.body = body;
	}

	@Override
	public void run()
	{
		HttpURLConnection connection = null;

		log.debug("posting \"" + url + "\"...");

		try
		{
			connection = getConnection();
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			connection.connect();
			OutputStream os = connection.getOutputStream();
			os.write(body.toString(2).getBytes());
			os.close();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"), 64);
				StringBuilder content = new StringBuilder();
				Io.copy(reader, content);
				JSONTokener tokener = new JSONTokener(content.toString());
				Object head = tokener.nextValue();
				handleResponse(head);
			} else {
				handleException(new RuntimeException("Got response " + connection.getResponseMessage()));
			}
		}
		catch (final Exception x) {
			handleException(x);
		} finally {
			if (connection != null)
				connection.disconnect();
		}
	}

	protected abstract void handleResponse(Object head);
}
