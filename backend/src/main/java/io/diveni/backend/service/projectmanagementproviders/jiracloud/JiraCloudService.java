/*
  SPDX-License-Identifier: AGPL-3.0-or-later
  Diveni - The Planing-Poker App
  Copyright (C) 2022 Diveni Team, AUME-Team 21/22, HTWG Konstanz
*/
package io.diveni.backend.service.projectmanagementproviders.jiracloud;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.util.Base64;

import io.diveni.backend.Utils;
import io.diveni.backend.model.Project;
import io.diveni.backend.model.TokenIdentifier;
import io.diveni.backend.model.UserStory;
import io.diveni.backend.service.projectmanagementproviders.ProjectManagementProviderOAuth2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import io.diveni.backend.controller.ErrorMessages;
import lombok.Getter;
import lombok.val;

import javax.annotation.PostConstruct;

@Service
public class JiraCloudService implements ProjectManagementProviderOAuth2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraCloudService.class);
    private static final int JIRA_CLOUD_API_VERSION = 2;
    private static final String JIRA_OAUTH_URL = "https://auth.atlassian.com/oauth";
    private static final String JIRA_HOME = "https://api.atlassian.com/ex/jira/%s/rest/api/";
    private static final String JIRA_BASE_ISSUE_LINK = "%s/browse/%s";
    private boolean serviceEnabled = false;
    @Getter
    private final Map<String, String> accessTokens = new HashMap<>();

    @Value("${JIRA_CLOUD_CLIENTID:#{null}}")
    private String CLIENT_ID;

    @Value("${JIRA_CLOUD_CLIENTSECRET:#{null}}")
    private String CLIENT_SECRET;

    @Value("${JIRA_CLOUD_ESTIMATIONFIELD:customfield_10016}")
    private String ESTIMATION_FIELD;

    @Value("${JIRA_CLOUD_AUTHORIZE_URL:#{null}}")
    private String JIRA_CLOUD_AUTHORIZE_URL;

    @PostConstruct
    public void setupAndLogConfig() {
        if (CLIENT_ID != null
                && CLIENT_SECRET != null
                && ESTIMATION_FIELD != null
                && JIRA_CLOUD_AUTHORIZE_URL != null) {
            serviceEnabled = true;
        }

        LOGGER.info("Jira-Cloud Service: (enabled:" + serviceEnabled + ")");

        LOGGER.info("    JIRA_CLOUD_CLIENTID={}", CLIENT_ID == null ? "null" : "********");
        LOGGER.info("    JIRA_CLOUD_CLIENTSECRET={}", CLIENT_SECRET == null ? "null" : "********");
        LOGGER.info("    JIRA_SERVER_ESTIMATIONFIELD={}", ESTIMATION_FIELD);
        LOGGER.info("    JIRA_CLOUD_AUTHORIZE_URL={}", JIRA_CLOUD_AUTHORIZE_URL);
    }

    @Override
    public boolean serviceEnabled() {
        return serviceEnabled;
    }

    public String getJiraCloudAuthorizeUrl() {
        return JIRA_CLOUD_AUTHORIZE_URL;
    }

    static ObjectNode[] getResources(String accessToken) {
        LOGGER.debug("--> getResources()");
        String accessibleResourcesURL = "https://api.atlassian.com/oauth/token/accessible-resources";
        ResponseEntity<String> response =
                executeRequest(accessibleResourcesURL, HttpMethod.GET, accessToken, null);
        try {
            LOGGER.debug("<-- getResources()");
            return new ObjectMapper().readValue(response.getBody(), ObjectNode[].class);
        } catch (Exception ex) {
            LOGGER.error("Failed to get resources", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.failedToRetrieveAccessTokenErrorMessage);
        }
    }

    static ObjectNode getResourceByFieldValue(String fieldName, String value, String accessToken) {
        ObjectNode[] resources = getResources(accessToken);
        ObjectNode result = null;
        for (ObjectNode resource : resources) {
            if (resource.has(fieldName) && resource.get(fieldName).asText().equals(value)) {
                result = resource;
            }
        }
        if(result == null){
            LOGGER.error(ErrorMessages.resourceNotFound);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.resourceNotFound);
        }
        return result;
    }


    static String extractField(String field, ObjectNode node) {
        if (node.has(field)) {
            return node.get(field).asText();
        }
        return null;
    }

    static String getDomainUrl(ObjectNode resource) {
        LOGGER.debug("--> getDomainUrl()");
        String result = extractField("url", resource);
        if (result != null) {
            LOGGER.debug("<-- getDomainUrl()");
            return result;
        }
        LOGGER.debug("<-- getDomainUrl(), url not found");
        return null;
    }

    static String getCloudID(ObjectNode resource) {
        LOGGER.debug("--> getCloudID()");
        String result = extractField("id", resource);
        if (result != null) {
            LOGGER.debug("<-- getCloudID()");
            return result;
        }
        LOGGER.debug("<-- getCloudID(), id not found");
        return null;

    }

    static ResponseEntity<String> executeRequest(
            String url, HttpMethod method, String accessToken, Object body) {
        LOGGER.debug("--> executeRequest()");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        LOGGER.debug("<-- executeRequest()");
        return restTemplate.exchange(url, method, request, String.class);
    }

    @Override
    public TokenIdentifier getAccessToken(String authorizationCode, String origin) {
        LOGGER.debug("--> getAccessToken()");
        RestTemplate restTemplate = new RestTemplate();
        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String encodedCredentials = new String(Base64.encodeBase64(credentials.getBytes()));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic " + encodedCredentials);

        HttpEntity<String> request = new HttpEntity<>(headers);

    String accessTokenURL = JIRA_OAUTH_URL + "/token";
    accessTokenURL += "?code=" + authorizationCode;
    accessTokenURL += "&grant_type=authorization_code";
    accessTokenURL += "&redirect_uri=" + origin + "/#/jiraCallback";

        ResponseEntity<String> response =
                restTemplate.exchange(accessTokenURL, HttpMethod.POST, request, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node;
        try {
            node = mapper.readTree(response.getBody());
            String accessToken = node.path("access_token").asText();
            val id = Utils.generateRandomID();
            accessTokens.put(id, accessToken);
            LOGGER.debug("<-- getAccessToken()");
            return new TokenIdentifier(id);
        } catch (Exception e) {
            LOGGER.error("Failed to get access token!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.failedToRetrieveAccessTokenErrorMessage);
        }
    }

    @Override
    public List<Project> getProjects(String tokenIdentifier, Map<String, String[]> reqParams) {
        String site;
        if(!reqParams.containsKey("site")){
            LOGGER.error(ErrorMessages.missingSiteUrl);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,ErrorMessages.missingSiteUrl);
        }else {
            String[] sitesParam = reqParams.get("site");
            if(sitesParam.length > 1){
                LOGGER.error(ErrorMessages.wrongSiteParamLength);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,ErrorMessages.wrongSiteParamLength);
            }
            site = sitesParam[0];

        }
        LOGGER.debug("--> getProjects()");
        val accessToken = accessTokens.get(tokenIdentifier);
        ObjectNode resource = getResourceByFieldValue("url", site, accessToken);
        String cloudID = getCloudID(resource);
        try {
            List<Project> projects = new ArrayList<>();
            ResponseEntity<String> response =
                    executeRequest(
                            String.format(getJiraUrl(), cloudID) + "/project/search",
                            HttpMethod.GET,
                            accessToken,
                            null);
            JsonNode node = new ObjectMapper().readTree(response.getBody());

            for (JsonNode projectNode : node.path("values")) {
                projects.add(new Project(projectNode.get("name").asText(), projectNode.get("id").asText()));
            }
            LOGGER.debug("<-- getProjects()");
            return projects;
        } catch (Exception e) {
            LOGGER.error("Failed to get projects!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.failedToRetrieveProjectsErrorMessage);
        }
    }

    @Override
    public List<String> getSites(String tokenIdentifier) {
        LOGGER.debug("--> getSites()");
        String accessToken = accessTokens.get(tokenIdentifier);
        ObjectNode[] resources = getResources(accessToken);
        List<String> sites = new ArrayList<>();
        for (ObjectNode resource : resources) {
            if (resource.has("url")) {
                sites.add(resource.get("url").asText());
            }
        }
        LOGGER.debug("<-- getSites()");
        return sites;
    }

    @Override
    public List<UserStory> getIssues(String tokenIdentifier, String projectName) {
        LOGGER.debug("--> getIssues(), projectName={}", projectName);
        String accessToken = accessTokens.get(tokenIdentifier);
        ObjectNode resource = getResourceByFieldValue("url", "tobefixed", accessToken);

        String cloudID = getCloudID(resource);
        String url = getDomainUrl(resource);
        ResponseEntity<String> response =
                executeRequest(
                        String.format(getJiraUrl(), cloudID)
                                + "/search?jql=project='"
                                + projectName
                                + "' order by rank&fields=summary,description,"
                                + ESTIMATION_FIELD,
                        HttpMethod.GET,
                        accessToken,
                        null);
        try {
            List<UserStory> userStories = new ArrayList<>();
            JsonNode node = new ObjectMapper().readTree(response.getBody());
            for (JsonNode issue : node.path("issues")) {
                val fields = issue.get("fields");
                String estimation =
                        fields.get(ESTIMATION_FIELD).isNull()
                                ? null
                                : String.valueOf(fields.get(ESTIMATION_FIELD).asDouble());
                if (estimation != null && estimation.endsWith(".0")) {
                    estimation = estimation.substring(0, estimation.length() - 2);
                }
                String issueKey = issue.get("key").textValue();
                userStories.add(
                        new UserStory(
                                issue.get("id").textValue(),
                                issueKey,
                                createIssueLink(url, issueKey),
                                fields.get("summary").textValue(),
                                fields.get("description").textValue(),
                                estimation,
                                false));
            }
            LOGGER.debug("<-- getIssues()");
            return userStories;
        } catch (Exception e) {
            LOGGER.error("Failed to get issues!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.failedToRetrieveProjectsErrorMessage);
        }
    }

    @Override
    public void updateIssue(String tokenIdentifier, UserStory story) {
        LOGGER.debug("--> updateIssue(), storyID={}", story.getId());
        String accessToken = accessTokens.get(tokenIdentifier);
        ObjectNode resource = getResourceByFieldValue("url", "tobefixed", accessToken);
        String cloudID = getCloudID(resource);
        Map<String, Map<String, Object>> content = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        fields.put("summary", story.getTitle());
        fields.put("description", story.getDescription());
        if (story.getEstimation() != null) {
            try {
                fields.put(ESTIMATION_FIELD, Double.parseDouble(story.getEstimation()));
            } catch (NumberFormatException e) {
                LOGGER.error("Failed to parse estimation into double!");
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.failedToEditIssueErrorMessage);
            }
        }
        content.put("fields", fields);
        try {
            executeRequest(
                    String.format(getJiraUrl(), cloudID) + "/issue/" + story.getId(),
                    HttpMethod.PUT,
                    accessTokens.get(tokenIdentifier),
                    content);
            LOGGER.debug("<-- updateIssue()");
        } catch (Exception e) {
            LOGGER.error("Failed to update issue!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.failedToEditIssueErrorMessage);
        }
    }

    @Override
    public void deleteIssue(String tokenIdentifier, String issueID) {
        LOGGER.debug("--> deleteIssue(), issueID={}", issueID);
        String accessToken = accessTokens.get(tokenIdentifier);
        try {
            ObjectNode resource = getResourceByFieldValue("url", "tobefixed", accessToken);

            String cloudID = getCloudID(resource);
            executeRequest(
                    String.format(getJiraUrl(), cloudID) + "/issue/" + issueID,
                    HttpMethod.DELETE,
                    accessTokens.get(tokenIdentifier),
                    null);
            LOGGER.debug("<-- deleteIssue()");
        } catch (Exception e) {
            LOGGER.error("Failed to delete issue!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.failedToEditIssueErrorMessage);
        }
    }

    @Override
    public String getCurrentUsername(String tokenIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean containsToken(String token) {
        return accessTokens.containsKey(token);
    }

    @Override
    public String createIssue(String tokenIdentifier, String projectID, UserStory story) {
        // TODO Auto-generated method stub
        return null;
    }

    private String getJiraUrl() {
        return JIRA_HOME + JIRA_CLOUD_API_VERSION;
    }

    private String createIssueLink(String url, String issueKey) {
        return String.format(JIRA_BASE_ISSUE_LINK, url, issueKey);
    }
}
