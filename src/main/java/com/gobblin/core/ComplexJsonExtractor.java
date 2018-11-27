package com.gobblin.core;

/**
 * Created by Anirudha Sathe on 22/11/18.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closer;

import org.apache.gobblin.configuration.ConfigurationKeys;
import org.apache.gobblin.configuration.WorkUnitState;
import org.apache.gobblin.password.PasswordManager;
import org.apache.gobblin.source.extractor.DataRecordException;
import org.apache.gobblin.source.extractor.Extractor;

/**
 * An implementation of {@link Extractor} for the simple JSON example.
 *
 * <p>
 *   This extractor uses the commons-vfs library to read the assigned input file storing
 *   json documents confirming to a schema. Each line of the file is a json document.
 * </p>
 *
 * @author Yinan Li
 */

public class ComplexJsonExtractor implements Extractor<String, String>{

    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexJsonExtractor.class);

    private static final String SOURCE_PAGE_KEY = "source.file";

    private final WorkUnitState workUnitState;

    private BufferedReader bufferedReader;

    private final HttpClient client;

    private final HttpGet request;

    private final HttpResponse response;

    private final Closer closer = Closer.create();

    public ComplexJsonExtractor(WorkUnitState workUnitState) throws FileSystemException, IOException {
        this.workUnitState = workUnitState;

        /*
        Custom logic for the extraction of data starts
         */

        this.client = new DefaultHttpClient();

        String url= workUnitState.getProp(SOURCE_PAGE_KEY);

        if (!workUnitState.getPropAsBoolean(ConfigurationKeys.SOURCE_CONN_USE_AUTHENTICATION, false)) {
            UserAuthenticator auth =
                    new StaticUserAuthenticator(workUnitState.getProp(ConfigurationKeys.SOURCE_CONN_DOMAIN, ""),
                            workUnitState.getProp(ConfigurationKeys.SOURCE_CONN_USERNAME), PasswordManager.getInstance(workUnitState)
                            .readPassword(workUnitState.getProp(ConfigurationKeys.SOURCE_CONN_PASSWORD)));

            this.request = new HttpGet(url);
        }else{
            this.request = new HttpGet(url);
        }

        this.response = this.client.execute(this.request);

        LOGGER.info("Extracting data from the URL provided.");

        if (this.response.getStatusLine().getStatusCode() == 200) {
            LOGGER.info("Response code returned - " + this.response.getStatusLine().getStatusCode());
            this.bufferedReader = this.closer.register(new BufferedReader(new InputStreamReader(this.response.getEntity().getContent(), ConfigurationKeys.DEFAULT_CHARSET_ENCODING)));
        }else{
            if (response.getStatusLine().getStatusCode() == 404){
                LOGGER.error("Error : " + response.getStatusLine().getStatusCode() + " Page not found error.");
            }else if (response.getStatusLine().getStatusCode() == 500) {
                LOGGER.error("Error : " + response.getStatusLine().getStatusCode() + " Internal Server error.");
            }else {
                LOGGER.error("Error : " + response.getStatusLine().getStatusCode() + " Unidentified.");
            }
        }

    }

    public String getSchema() {
        return this.workUnitState.getProp(ConfigurationKeys.SOURCE_SCHEMA);
    }

    public String readRecord(@Deprecated String reuse) throws DataRecordException, IOException {
        // Read the next line
        return this.bufferedReader.readLine();
    }

    public long getExpectedRecordCount() {
        // We don't know how many records are in the file before actually reading them
        return 0;
    }

    public long getHighWatermark() {
        // Watermark is not applicable for this type of extractor
        return 0;
    }

    public void close() throws IOException {
        try {
            this.closer.close();
        } catch (IOException ioe) {
            LOGGER.error("Failed to close the input stream", ioe);
        }
        this.request.abort();
    }
}
