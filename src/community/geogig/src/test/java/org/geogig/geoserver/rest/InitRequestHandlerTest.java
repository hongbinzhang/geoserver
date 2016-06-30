/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.geogig.geoserver.rest.InitRequestHandler.DB_HOST;
import static org.geogig.geoserver.rest.InitRequestHandler.DB_NAME;
import static org.geogig.geoserver.rest.InitRequestHandler.DB_PASSWORD;
import static org.geogig.geoserver.rest.InitRequestHandler.DB_PORT;
import static org.geogig.geoserver.rest.InitRequestHandler.DB_SCHEMA;
import static org.geogig.geoserver.rest.InitRequestHandler.DB_USER;
import static org.geogig.geoserver.rest.InitRequestHandler.DIR_PARENT_DIR;
import static org.geogig.geoserver.rest.InitRequestHandler.REPO_ATTR;
import static org.locationtech.geogig.repository.Hints.REPOSITORY_NAME;
import static org.locationtech.geogig.repository.Hints.REPOSITORY_URL;
import static org.restlet.data.MediaType.APPLICATION_JSON;
import static org.restlet.data.MediaType.APPLICATION_WWW_FORM;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;

import org.geogig.geoserver.config.PostgresConfigBean;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.rest.RestletException;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Representation;

import com.google.common.base.Optional;

public class InitRequestHandlerTest {

    @Rule
    public TemporaryFolder repoFolder = new TemporaryFolder();

    private final InitRequestHandler initRequestHandler = new InitRequestHandler();

    private Request buildRequest(Representation entity) {
        Request request = new Request(Method.PUT, "fake uri", entity);
        request.getAttributes().put(REPO_ATTR, "testRepo");
        return request;
    }

    private void assertRepositoryName(Hints hints) {
        Assert.assertTrue("Expected a Hints object to be created", hints != null);
        Assert.assertEquals("Incorrect Repository Name", "testRepo",
                hints.get(REPOSITORY_NAME).get());
    }

    @Test(expected = RestletException.class)
    public void testMissingRepositoryName() {
        Request request = new Request(Method.PUT, "fake uri");
        // build the Hints without a repository name getting in the attributes
        // should throw an Exception
        Hints hints = initRequestHandler.createHintsFromRequest(request);
    }

    @Test
    public void testCreateGeoGIG_RepositoryName() throws JSONException, IOException {
        // build an Init request with only a repository name
        Request request = buildRequest(null);
        // create the Hints from the request
        Hints hints = initRequestHandler.createHintsFromRequest(request);
        // assert the correct Repository Name is in the Hints
        assertRepositoryName(hints);
        // REPOSITORY_URI should NOT be set in the Hints, it should be generated by the Repository
        // Manager
        Optional<Serializable> repoURL = hints.get(REPOSITORY_URL);
        Assert.assertFalse("Expected REPOSIOTRY_URL to be ABSENT", repoURL.isPresent());
    }

    @Test
    public void testJSONMediaType() throws JSONException, IOException {
        // temp directory for the repo
        File repoDir = repoFolder.getRoot().getAbsoluteFile();
        // populate a JSON payload for a Directory repo
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(DIR_PARENT_DIR, repoDir.getAbsolutePath());
        JsonRepresentation entity = new JsonRepresentation(jsonObject);
        // ensure the Content-Type is JSON
        Assert.assertEquals("Bad MediaType", APPLICATION_JSON, entity.getMediaType());
        // build the request
        Request request = buildRequest(entity);
        // create the Hints from the Request
        Hints hints = initRequestHandler.createHintsFromRequest(request);
        // assert the correct Repository Name is in the Hints
        assertRepositoryName(hints);
        // REPOSITORY_URI should be set in the Hints
        Optional<Serializable> repoURL = hints.get(REPOSITORY_URL);
        Assert.assertTrue("Expected REPOSIOTRY_URL to be PRESENT", repoURL.isPresent());
        URI actual = URI.create(repoURL.get().toString());
        File actualParent = new File(actual).getParentFile();
        Assert.assertEquals("Repository Parent Directory does not match", repoDir, actualParent);
    }

    @Test
    public void testPGRepo() throws JSONException {
        // populate a JSON payload for a PG repo
        JSONObject jsonObject = new JSONObject();
        // add the DB attributes with no defaults at a minimum
        jsonObject.put(DB_NAME, "pgDatabaseName");
        jsonObject.put(DB_PASSWORD, "fakePassword");
        JsonRepresentation entity = new JsonRepresentation(jsonObject);
        // ensure the Content-Type is JSON
        Assert.assertEquals("Bad MediaType", APPLICATION_JSON, entity.getMediaType());
        // build the Request
        Request request = buildRequest(entity);
        // create the Hints from the Request
        Hints hints = initRequestHandler.createHintsFromRequest(request);
        // assert the correct Repository Name is in the Hints
        assertRepositoryName(hints);
        // REPOSITORY_URI should be set in the Hints
        Optional<Serializable> repoURL = hints.get(REPOSITORY_URL);
        Assert.assertTrue("Expected REPOSIOTRY_URL to be PRESENT", repoURL.isPresent());
        URI actual = URI.create(repoURL.get().toString());
        Assert.assertEquals("Unexpected URI Scheme", "postgresql", actual.getScheme());
        // default Postgres config
        final PostgresConfigBean expected = PostgresConfigBean.newInstance();
        // set the 2 attributes with no defaults
        expected.setDatabase("pgDatabaseName");
        expected.setPassword("fakePassword");
        final PostgresConfigBean actualBean = PostgresConfigBean.from(actual);
        Assert.assertEquals("Unexpected PG Configuration", expected, actualBean);
    }

    @Test
    public void testAllPGParameters() throws JSONException {
        // populate a JSON payload for a PG repo
        JSONObject jsonObject = new JSONObject();
        // add the DB attributes with no defaults at a minimum
        jsonObject.put(DB_NAME, "pgDatabaseName");
        jsonObject.put(DB_PASSWORD, "fakePassword");
        jsonObject.put(DB_SCHEMA, "fakeSchema");
        jsonObject.put(DB_USER, "fakeUser");
        jsonObject.put(DB_HOST, "fakeHost");
        jsonObject.put(DB_PORT, "8899");
        JsonRepresentation entity = new JsonRepresentation(jsonObject);
        // ensure the Content-Type is JSON
        Assert.assertEquals("Bad MediaType", APPLICATION_JSON, entity.getMediaType());
        // build the Request
        Request request = buildRequest(entity);
        // create the Hints from the Request
        Hints hints = initRequestHandler.createHintsFromRequest(request);
        // assert the correct Repository Name is in the Hints
        assertRepositoryName(hints);
        // REPOSITORY_URI should be set in the Hints
        Optional<Serializable> repoURL = hints.get(REPOSITORY_URL);
        Assert.assertTrue("Expected REPOSIOTRY_URL to be PRESENT", repoURL.isPresent());
        URI actual = URI.create(repoURL.get().toString());
        Assert.assertEquals("Unexpected URI Scheme", "postgresql", actual.getScheme());
        // default Postgres config
        final PostgresConfigBean expected = PostgresConfigBean.newInstance();
        // set the attributes we built into the JSON request
        expected.setDatabase("pgDatabaseName");
        expected.setPassword("fakePassword");
        expected.setHost("fakeHost");
        expected.setPort(Integer.valueOf("8899"));
        expected.setSchema("fakeSchema");
        expected.setUsername("fakeUser");
        final PostgresConfigBean actualBean = PostgresConfigBean.from(actual);
        Assert.assertEquals("Unexpected PG Configuration", expected, actualBean);
    }

    @Test
    public void testPGRepoBadPort() throws JSONException {
        // populate a JSON payload for a PG repo
        JSONObject jsonObject = new JSONObject();
        // add the DB attributes with no defaults at a minimum
        jsonObject.put(DB_NAME, "pgDatabaseName");
        jsonObject.put(DB_PASSWORD, "fakePassword");
        // fill in junk for port
        jsonObject.put(DB_PORT, "non-parsable integer");
        JsonRepresentation entity = new JsonRepresentation(jsonObject);
        // ensure the Content-Type is JSON
        Assert.assertEquals("Bad MediaType", APPLICATION_JSON, entity.getMediaType());
        // build the Request
        Request request = buildRequest(entity);
        // create the Hints from the Request
        Hints hints = initRequestHandler.createHintsFromRequest(request);
        // assert the correct Repository Name is in the Hints
        assertRepositoryName(hints);
        // REPOSITORY_URI should be set in the Hints
        Optional<Serializable> repoURL = hints.get(REPOSITORY_URL);
        Assert.assertTrue("Expected REPOSIOTRY_URL to be PRESENT", repoURL.isPresent());
        URI actual = URI.create(repoURL.get().toString());
        Assert.assertEquals("Unexpected URI Scheme", "postgresql", actual.getScheme());
        // default Postgres config
        final PostgresConfigBean expected = PostgresConfigBean.newInstance();
        // set the 2 attributes with no defaults, defualt PORT should be used
        expected.setDatabase("pgDatabaseName");
        expected.setPassword("fakePassword");
        final PostgresConfigBean actualBean = PostgresConfigBean.from(actual);
        Assert.assertEquals("Unexpected PG Configuration", expected, actualBean);
    }

    @Test
    public void testURLEncodedForm() {
        // build an Init request with only a repository name
        Request request = buildRequest(null);
        // set the form encoded data
        StringBuilder data = new StringBuilder(128);
        data.append(DB_NAME).append("=").append("pgDatabaseName").append("&");
        data.append(DB_PASSWORD).append("=").append("fakePassword").append("&");
        data.append(DB_SCHEMA).append("=").append("fakeSchema").append("&");
        data.append(DB_USER).append("=").append("fakeUser").append("&");
        data.append(DB_HOST).append("=").append("fakeHost").append("&");
        data.append(DB_PORT).append("=").append("8899");
        request.setEntity(data.toString(), APPLICATION_WWW_FORM);
        // create the Hints from the Request
        Hints hints = initRequestHandler.createHintsFromRequest(request);
        // assert the correct Repository Name is in the Hints
        assertRepositoryName(hints);
        // REPOSITORY_URI should be set in the Hints
        Optional<Serializable> repoURL = hints.get(REPOSITORY_URL);
        Assert.assertTrue("Expected REPOSIOTRY_URL to be PRESENT", repoURL.isPresent());
        URI actual = URI.create(repoURL.get().toString());
        Assert.assertEquals("Unexpected URI Scheme", "postgresql", actual.getScheme());
        // default Postgres config
        final PostgresConfigBean expected = PostgresConfigBean.newInstance();
        // set the attributes we built into the JSON request
        expected.setDatabase("pgDatabaseName");
        expected.setPassword("fakePassword");
        expected.setHost("fakeHost");
        expected.setPort(Integer.valueOf("8899"));
        expected.setSchema("fakeSchema");
        expected.setUsername("fakeUser");
        final PostgresConfigBean actualBean = PostgresConfigBean.from(actual);
        Assert.assertEquals("Unexpected PG Configuration", expected, actualBean);
    }
}
